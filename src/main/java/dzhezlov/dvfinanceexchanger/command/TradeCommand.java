package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.config.LimitProperties;
import dzhezlov.dvfinanceexchanger.repository.CommandHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.CommandHistory;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.CommandUtils.toUserId;

@Component
@RequiredArgsConstructor
public class TradeCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final CommandHistoryRepository commandHistoryRepository;
    private final MessageCleaner messageCleaner;
    private final LimitProperties limitProperties;

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
            UserId recipient = toUserId(message);
            List<TradeHistory> tradeHistories = tradeHistoryRepository.findByTradeInitiator(recipient);

            if (isLimitExchangeAvailable(recipient)) {
                int countExchanges = tradeHistories.size();
                long uniqueSenders = tradeHistories.stream()
                        .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                        .filter(userId -> !userId.equals(recipient))
                        .distinct()
                        .count();

                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());

                StringBuilder answerText = new StringBuilder()
                        .append("Обменов: ")
                        .append(countExchanges)
                        .append("\nС участниками/людьми: ")
                        .append(uniqueSenders);
                answer.setText(answerText.toString());

                absSender.execute(answer);
            } else {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.setReplyToMessageId(message.getMessageId());
                answer.setText("Не флудим, вызываем команду раз в " + limitProperties.getTrade().toDays() + " дня");

                Message sentMessage = absSender.execute(answer);
                messageCleaner.cleanAfterDelay(absSender, sentMessage);
            }

            commandHistoryRepository.save(
                    CommandHistory.builder()
                            .userId(recipient)
                            .timestamp(Instant.now())
                            .command(getCommandIdentifier())
                            .build()
            );
        }
    }

    private boolean isLimitExchangeAvailable(UserId recipient) {
        List<CommandHistory> commands = commandHistoryRepository.findByUserId(recipient).stream()
                .filter(command -> command.getCommand().equals(getCommandIdentifier()))
                .sorted(Comparator.comparing(CommandHistory::getTimestamp))
                .collect(Collectors.toList());

        if (commands.isEmpty()) {
            return true;
        }

        CommandHistory lastExchange = commands.get(commands.size() - 1);

        Instant plus = lastExchange.getTimestamp().plus(limitProperties.getTrade());

        return plus.truncatedTo(ChronoUnit.DAYS)
                .isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS));
    }
}
