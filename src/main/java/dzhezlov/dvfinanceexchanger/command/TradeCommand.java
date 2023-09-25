package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.config.TradeProperties;
import dzhezlov.dvfinanceexchanger.repository.CommandHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TrustUserRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.CommandHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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
            List<TradeHistory> tradeHistories = tradeHistoryRepository.findByParticipantsUserIdIn(userId)
                    .stream()
                    .filter(tradeHistory ->
                            tradeHistory.getParticipants().stream()
                                    .allMatch(Participant::isApproveTrade)
                    )
                    .collect(Collectors.toList());

            if (isLimitTradesAvailable(userId)) {
                int countExchanges = tradeHistories.size();
                long uniqueSenders = tradeHistories.stream()
                        .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                        .filter(participant -> participant.getUserId().equals(userId))
                        .distinct()
                        .count();

                StringBuilder answerText = new StringBuilder()
                        .append("Обменов: ")
                        .append(countExchanges)
                        .append("\nС участниками:  ")
                        .append(uniqueSenders);
                trustUserRepository.findById(userId)
                        .ifPresent(user -> answerText.append("\nМожно доверять: ✅"));

                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText(answerText.toString());

                absSender.execute(answer);
            } else {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText("Не флудим, вызываем команду раз в " + tradeProperties.getTrade().toDays() + " дня");

                Message sentMessage = absSender.execute(answer);
                messageCleaner.cleanAfterDelay(absSender, sentMessage);
                messageCleaner.cleanAfterDelay(absSender, message);
            }

            commandHistoryRepository.save(
                    CommandHistory.builder()
                            .userId(userId)
                            .timestamp(Instant.now())
                            .command(getCommandIdentifier())
                            .build()
            );
        } else {
            messageCleaner.cleanAfterDelay(absSender, message, 3);
        }
    }

    private boolean isLimitTradesAvailable(UserId recipient) {
        Optional<CommandHistory> lastTrade =
                commandHistoryRepository.findFirstByUserIdAndCommandOrderByTimestampDesc(recipient, getCommandIdentifier());

        if (lastTrade.isEmpty()) {
            return true;
        }

        Instant plus = lastTrade.get().getTimestamp().plus(tradeProperties.getTrade());

        return plus.truncatedTo(ChronoUnit.DAYS)
                .isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }
}
