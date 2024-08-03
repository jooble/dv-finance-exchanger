package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.command.utils.FormatUtils;
import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TrustUserRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.TrustUser;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import dzhezlov.dvfinanceexchanger.service.UserJoinedService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.*;
import static dzhezlov.dvfinanceexchanger.command.utils.FormatUtils.toMention;
import static dzhezlov.dvfinanceexchanger.command.utils.FormatUtils.toUserFullName;

@Component
@RequiredArgsConstructor
public class CheckCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final TrustUserRepository trustUserRepository;
    private final MessageCleaner messageCleaner;
    private final UserJoinedService userJoinedService;

    @Override
    public String getCommandIdentifier() {
        return "check";
    }

    @Override
    public String getDescription() {
        return "check";
    }

    @SneakyThrows
    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        if (message.isReply() && isNotFromBot(message)) {
            UserId recipient = toUserId(message.getReplyToMessage());
            List<TradeHistory> tradeHistories = tradeHistoryRepository.findByParticipantsUserIdIn(recipient)
                    .stream()
                    .filter(tradeHistory ->
                            tradeHistory.getParticipants().stream()
                                    .allMatch(Participant::isApproveTrade)
                    )
                    .collect(Collectors.toList());

            int countTrades = tradeHistories.size();
            long uniqueSenders = tradeHistories.stream()
                    .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                    .filter(participant -> !participant.getUserId().equals(recipient))
                    .distinct()
                    .count();
            Set<String> previousFullNames = tradeHistories.stream()
                    .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                    .filter(participant -> participant.getUserId().equals(recipient))
                    .map(Participant::getFullName)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());

            StringBuilder answerText = new StringBuilder()
                    .append("Участник: ")
                    .append(toMention(message.getReplyToMessage().getFrom()))
                    .append("\nОбменов: ")
                    .append(countTrades)
                    .append("\nС уникальными участниками: ")
                    .append(uniqueSenders);
            Optional<TrustUser> trustUser = trustUserRepository.findById(recipient);

            if (!previousFullNames.isEmpty()) {
                answerText.append("\n\nПредыдущие имена: ")
                        .append(FormatUtils.toList(previousFullNames));
            }

            if (trustUser.isPresent()) {
                answerText.append("\n\nМожно доверять: ✅");

                if (isAdminMessage(message, absSender)) {
                    User adminUser = fetchUserByUserId(trustUser.orElseThrow().getSender(), absSender);
                    answerText.append("by ");
                    answerText.append(toUserFullName(adminUser));
                }
            }

            if (isAdminMessage(message, absSender)) {
                Instant dataJoined = userJoinedService.getDataJoined(recipient);
                answerText.append("\nДата входа: " + ChronoUnit.DAYS.between(dataJoined, Instant.now()) + " дня назад");
            }

            SendMessage answer = new SendMessage();
            answer.setChatId(message.getChatId());
            answer.setReplyToMessageId(message.getMessageId());
            answer.setText(answerText.toString());
            answer.enableHtml(true);

            Message sentMessage = absSender.execute(answer);
            messageCleaner.cleanAfterDelay(absSender, sentMessage);
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }
}
