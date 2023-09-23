package dzhezlov.dvfinanceexchanger;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Getter
@Component
public class DVFinanceExchangerBot extends TelegramLongPollingCommandBot {

    private final String botToken;
    private final String botUsername;

    public DVFinanceExchangerBot(@Value("${spring.telegram.token}") String botToken,
                                 @Value("${spring.name}") String botUsername,
                                 List<IBotCommand> commands) {
        super(botToken);

        this.botToken = botToken;
        this.botUsername = botUsername;

        commands.forEach(super::register);
    }

    @Override
    public void processNonCommandUpdate(Update update) {
    }
}
