package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.TrustUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.isAdminMessage;
import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.toUserId;

@Component
@RequiredArgsConstructor
public class UnTrustCommand implements IBotCommand {

    private final TrustUserRepository trustUserRepository;
    private final MessageCleaner messageCleaner;

    @Override
    public String getCommandIdentifier() {
        return "untrust";
    }

    @Override
    public String getDescription() {
        return "untrust";
    }

    @SneakyThrows
    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        if (message.isReply()
                && message.isSuperGroupMessage()
                && isAdminMessage(message, absSender)) {
            trustUserRepository.deleteById(toUserId(message.getReplyToMessage()));

            SendMessage answer = new SendMessage();
            answer.setChatId(message.getChatId());
            answer.setReplyToMessageId(message.getMessageId());
            answer.setText("ok");

            Message sentMessage = absSender.execute(answer);
            messageCleaner.cleanAfterDelay(absSender, sentMessage);
            messageCleaner.cleanAfterDelay(absSender, message);

            messageCleaner.cleanAfterDelay(absSender, sentMessage, 3);
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }
}
