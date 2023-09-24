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
import java.util.List;
import java.util.Optional;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.isNotReplyMyself;
import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.toUserId;
import static dzhezlov.dvfinanceexchanger.command.utils.FormatUtils.toMention;

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

            if (isSomeDayRetry(sender)) {
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.enableHtml(true);
                answer.setText("Вы сегодня уже прозводили обмен с " + toMention(message.getReplyToMessage().getFrom()));

                Message sentMessage = absSender.execute(answer);
                messageCleaner.cleanAfterDelay(absSender, sentMessage);
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

                absSender.execute(answer);
            }
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }

    private boolean isSomeDayRetry(UserId sender) {
        Optional<TradeHistory> lastTradeHistory =
                tradeHistoryRepository.findFirstByParticipantsInOrderByTimestampDesc(sender);

        if (lastTradeHistory.isEmpty()) return false;

        Instant lastTimestamp = lastTradeHistory.get().getTimestamp();

        return lastTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                .isEqual(LocalDate.now());
    }
}
