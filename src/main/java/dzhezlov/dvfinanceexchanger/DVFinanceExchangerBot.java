package dzhezlov.dvfinanceexchanger;

import dzhezlov.dvfinanceexchanger.command.CallbackCommand;
import dzhezlov.dvfinanceexchanger.service.UserJoinedService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Getter
@Component
public class DVFinanceExchangerBot extends TelegramLongPollingCommandBot {

    private final String botToken;
    private final String botUsername;
    private final UserJoinedService userJoinedService;

    public DVFinanceExchangerBot(@Value("${spring.telegram.token}") String botToken,
                                 @Value("${spring.name}") String botUsername,
                                 List<IBotCommand> commands,
                                 UserJoinedService userJoinedService) {
        super(botToken);

        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userJoinedService = userJoinedService;

        commands.forEach(super::register);
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            getRegisteredCommands().stream()
                    .filter(iBotCommand -> iBotCommand instanceof CallbackCommand)
                    .filter(iBotCommand -> ((CallbackCommand) iBotCommand).isCanProcess(this, update))
                    .forEach(iBotCommand -> ((CallbackCommand) iBotCommand).processCallback(this, update));
        }

        if (update.hasMessage()) {
            userJoinedService.processMessage(update.getMessage());
        }
    }

    @Override
    public void processInvalidCommandUpdate(Update update) {
        //trick that avoid command case
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.isCommand()) {
                String lowerTextMessage = message.getText().toLowerCase();
                if (!message.getText().equals(lowerTextMessage)) { // we want handle it only once
                    message.setText(lowerTextMessage); // doesn't mean if it contains @mention
                    update.setMessage(message);

                    onUpdateReceived(update);
                }
            }
        }
    }
}
