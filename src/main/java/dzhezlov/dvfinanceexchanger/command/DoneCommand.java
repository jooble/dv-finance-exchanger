package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.CommandUtils.*;

@Component
@RequiredArgsConstructor
public class DoneCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final MessageCleaner messageCleaner;

    @Override
    public String getCommandIdentifier() {
        return "done";
    }

    @Override
    public String getDescription() {
        return "done";
    }

    @SneakyThrows
    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        if (message.isReply() && isNotReplyMyself(message)) {
            UserId tradeInitiator = toUserId(message.getReplyToMessage());
            UserId sender = toUserId(message);
            Message sentMessage;

            if (isSomeDayRetry(sender)) {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.enableHtml(true);
                answer.setText("Вы уже сегодня прозводили обмен с " + toMention(message.getReplyToMessage().getFrom()));

                sentMessage = absSender.execute(answer);
            } else {

                tradeHistoryRepository.save(
                        TradeHistory.builder()
                                .tradeInitiator(tradeInitiator)
                                .doneSender(sender)
                                .participants(List.of(tradeInitiator, sender))
                                .timestamp(Instant.now())
                                .build()
                );

                StringBuilder answerText = new StringBuilder()
                        .append("Спасибо, произведен обмен между ")
                        .append(toMention(message.getReplyToMessage().getFrom()))
                        .append(" и ")
                        .append(toMention(message.getFrom()));
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.enableHtml(true);
                answer.setText(answerText.toString());

                sentMessage = absSender.execute(answer);
            }
            messageCleaner.cleanAfterDelay(absSender, sentMessage);
        }
    }

    private boolean isSomeDayRetry(UserId sender) {
        List<TradeHistory> tradeHistories = tradeHistoryRepository.findByParticipantsIn(sender).stream()
                .sorted(Comparator.comparing(TradeHistory::getTimestamp))
                .collect(Collectors.toList());

        if (tradeHistories.isEmpty()) return false;

        Instant lastTimeStamp = tradeHistories.get(tradeHistories.size() - 1).getTimestamp();

        return lastTimeStamp.atZone(ZoneId.systemDefault()).toLocalDate()
                .isEqual(LocalDate.now());
    }
}
