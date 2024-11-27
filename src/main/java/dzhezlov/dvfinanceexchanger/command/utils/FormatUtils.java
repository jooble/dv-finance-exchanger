package dzhezlov.dvfinanceexchanger.command.utils;

import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class FormatUtils {
    public static String toMention(User user) {
        return "<a href='tg://user?id=" +
                user.getId() +
                "'>" +
                toUserFullName(user) +
                "</a>";
    }

    public static String toMention(Participant participant) {
        return "<a href='tg://user?id=" +
                participant.getUserId().getUserId() +
                "'>" +
                participant.getFullName() +
                "</a>";
    }

    public static String toUserFullName(User user) {
        StringBuilder builder = new StringBuilder()
                .append(trimToEmpty(user.getFirstName()));

        if (StringUtils.isNotEmpty(user.getLastName())) {
            builder.append(" ")
                    .append(trimToEmpty(user.getLastName()));
        }

        if (user.getUserName() != null) {
            builder.append(" (@")
                    .append(user.getUserName())
                    .append(")");
        }

        return builder.toString();
    }

    public static String toList(Set<String> set) {
        StringBuilder builder = new StringBuilder();

        for (String item : set) {
            builder.append("\n • ").append(item);
        }

        return builder.toString();
    }
}
