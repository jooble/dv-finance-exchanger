package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.command.utils.FormatUtils;
import dzhezlov.dvfinanceexchanger.config.TradeProperties;
import dzhezlov.dvfinanceexchanger.repository.CommandHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TrustUserRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.CommandHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import dzhezlov.dvfinanceexchanger.service.UserJoinedService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.toUserId;

@Component
@RequiredArgsConstructor
public class TradeCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final CommandHistoryRepository commandHistoryRepository;
    private final TrustUserRepository trustUserRepository;
    private final MessageCleaner messageCleaner;
    private final TradeProperties tradeProperties;
    private final UserJoinedService userJoinedService;

    @Override
    public String getCommandIdentifier() {
        return "trade";
    }

    @Override
    public String getDescription() {
        return "trade";
    }

    @SneakyThrows
    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        if (ArrayUtils.isNotEmpty(arguments)) {
            UserId userId = toUserId(message);
            Instant dataJoined = userJoinedService.getDataJoined(userId);

            List<TradeHistory> tradeHistories = tradeHistoryRepository.findByParticipantsUserIdIn(userId)
                    .stream()
                    .filter(tradeHistory ->
                            tradeHistory.getParticipants().stream()
                                    .allMatch(Participant::isApproveTrade)
                    )
                    .collect(Collectors.toList());

            if (dataJoined.isAfter(Instant.now().minus(tradeProperties.getFirstTimeout()))) {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText("Инициация обмена разрешена не ранее " + tradeProperties.getFirstTimeout().toDays() + " дней после вступления в чат.");

                Message sentMessage = absSender.execute(answer);
                clean(absSender, sentMessage, message);
                return;
            }

            if (isLimitTradesAvailable(userId)) {
                int countExchanges = tradeHistories.size();
                long uniqueSenders = tradeHistories.stream()
                        .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                        .filter(participant -> !participant.getUserId().equals(userId))
                        .distinct()
                        .count();
                Set<String> previousFullNames = tradeHistories.stream()
                        .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                        .filter(participant -> participant.getUserId().equals(userId))
                        .map(Participant::getFullName)
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toSet());

                StringBuilder answerText = new StringBuilder()
                        .append("Обменов: ")
                        .append(countExchanges)
                        .append("\nС уникальными участниками: ")
                        .append(uniqueSenders);

                if (!previousFullNames.isEmpty()) {
                    answerText.append("\n\nПредыдущие имена: ")
                            .append(FormatUtils.toList(previousFullNames));
                }

                trustUserRepository.findById(userId)
                        .ifPresent(user -> answerText.append("\n\nМожно доверять: ✅"));

                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText(answerText.toString());
                answer.enableHtml(true);

                absSender.execute(answer);

                commandHistoryRepository.save(
                        CommandHistory.builder()
                                .userId(userId)
                                .timestamp(Instant.now())
                                .command(getCommandIdentifier())
                                .build()
                );
            } else {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText("Не флудим, вызываем команду раз в " + tradeProperties.getTimeout().plusDays(1).toDays() + " дня");

                Message sentMessage = absSender.execute(answer);
                clean(absSender, message, sentMessage);
            }
        } else {
            messageCleaner.cleanAfterDelay(absSender, message, 3);
        }
    }

    private void clean(AbsSender absSender, Message... messages) {
        for (Message message : messages) {
            messageCleaner.cleanAfterDelay(absSender, message);
        }
    }

    private boolean isLimitTradesAvailable(UserId recipient) {
        Optional<CommandHistory> lastTrade =
                commandHistoryRepository.findFirstByUserIdAndCommandOrderByTimestampDesc(recipient, getCommandIdentifier());

        if (lastTrade.isEmpty()) {
            return true;
        }

        Instant plus = lastTrade.get().getTimestamp().plus(tradeProperties.getTimeout());

        return plus.truncatedTo(ChronoUnit.DAYS)
                .isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }
}
