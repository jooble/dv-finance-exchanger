package dzhezlov.dvfinanceexchanger.command.utils;

import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.ArrayList;

public class CommandUtils {

    public static boolean isNotReplyMyself(Message message) {
        Long replyUserId = message.getReplyToMessage().getFrom().getId();
        Long messageUserId = message.getFrom().getId();

        return !replyUserId.equals(messageUserId);
    }

    public static Boolean isNotFromBot(Message message) {
        return !message.getReplyToMessage().getFrom().getIsBot();
    }

    public static UserId toUserId(Message message) {
        return UserId.builder()
                .userId(message.getFrom().getId())
                .chatId(message.getChatId())
                .build();
    }

    @SneakyThrows
    public static boolean isAdminMessage(Message message, AbsSender absSender) {
        ArrayList<ChatMember> administrators = absSender.execute(GetChatAdministrators.builder()
                .chatId(message.getChatId())
                .build());

        return administrators.stream()
                .anyMatch(admin -> admin.getUser().getId().equals(message.getFrom().getId()));
    }
}
