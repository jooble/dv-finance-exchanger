package dzhezlov.dvfinanceexchanger.command.utils;

import org.telegram.telegrambots.meta.api.objects.User;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class FormatUtils {
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
