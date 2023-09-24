package dzhezlov.dvfinanceexchanger.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MessageCleaner {
    private final ScheduledExecutorService messageCleanerPool;

    public void cleanAfterDelay(AbsSender absSender, Message message) {
        cleanAfterDelay(absSender, message, 30);
    }

    public void cleanAfterDelay(AbsSender absSender, Message message, int secs) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(message.getChatId());
        deleteMessage.setMessageId(message.getMessageId());

        messageCleanerPool.schedule(() -> absSender.execute(deleteMessage), 30, TimeUnit.SECONDS);
    }
}
