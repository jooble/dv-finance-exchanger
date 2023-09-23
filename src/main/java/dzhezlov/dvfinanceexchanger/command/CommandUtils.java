package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class CommandUtils {

    public static boolean isNotReplyMyself(Message message) {
        Long replyUserId = message.getReplyToMessage().getFrom().getId();
        Long messageUserId = message.getFrom().getId();

        return !replyUserId.equals(messageUserId);
    }

    public static UserId toUserId(Message message) {
        return UserId.builder()
                .userId(message.getFrom().getId())
                .chatId(message.getChatId())
                .build();
    }

    public static String toMention(User user) {
        return "<a href='tg://user?id=" +
                user.getId() +
                "'>" +
                toUserFullName(user) +
                "</a>";
    }

    public static String toUserFullName(User user) {
        StringBuilder builder = new StringBuilder()
                .append(trimToEmpty(user.getFirstName()))
                .append(trimToEmpty(user.getLastName()));

        if (user.getUserName() != null) {
            builder.append(" (@")
                    .append(user.getUserName())
                    .append(")");
        }

        return builder.toString();
    }
}
