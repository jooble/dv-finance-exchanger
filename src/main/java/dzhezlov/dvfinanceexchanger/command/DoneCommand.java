package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.*;
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
        if (message.isReply() && isNotReplyMyself(message) && isNotFromBot(message)) {
            UserId replyUserId = toUserId(message.getReplyToMessage());
            UserId userId = toUserId(message);

            if (isSomeDayRetry(userId, replyUserId)) {
                processFlood(absSender, message);
            } else if (isApproveFromSecondSide(userId, replyUserId)) {
                processApproveFromSecondSide(absSender, message, userId);
            } else {
                processFirstApprove(absSender, message, userId, replyUserId);
            }
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }

    private void processFirstApprove(AbsSender absSender, Message message, UserId userId, UserId replyUserId) throws TelegramApiException {
        tradeHistoryRepository.save(
                TradeHistory.builder()
                        .participants(List.of(
                                Participant.builder()
                                        .userId(userId)
                                        .approveTrade(true)
                                        .build(),
                                Participant.builder()
                                        .userId(replyUserId)
                                        .build()
                        ))
                        .timestamp(Instant.now())
                        .expireTime(LocalDateTime.now().plusDays(7).toInstant(OffsetDateTime.now().getOffset()))
                        .build()
        );

        StringBuilder answerText = new StringBuilder()
                .append(toMention(message.getFrom()))
                .append(" просит подтвердить обмен с ")
                .append(toMention(message.getReplyToMessage().getFrom()))
                .append(", отправив команду ")
                .append("<code>/")
                .append(getCommandIdentifier())
                .append("</code>")
                .append(", в ответ на его сообщение об обмене");
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.enableHtml(true);
        answer.setText(answerText.toString());

        Message sentMessage = absSender.execute(answer);
        messageCleaner.cleanAfterDelay(absSender, sentMessage, 600);
    }

    private void processApproveFromSecondSide(AbsSender absSender, Message message, UserId userId) throws TelegramApiException {
        TradeHistory lastTradeHistory =
                tradeHistoryRepository.findByParticipantsUserIdInOrderByTimestampDesc(userId)
                        .stream().findFirst().get();

        Participant sameParticipant = lastTradeHistory.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .get();

        sameParticipant.setApproveTrade(true);
        lastTradeHistory.setExpireTime(null);

        tradeHistoryRepository.save(lastTradeHistory);

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

    private void processFlood(AbsSender absSender, Message message) throws TelegramApiException {
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.enableHtml(true);
        answer.setText("Вы сегодня уже прозводили обмен с " + toMention(message.getReplyToMessage().getFrom()));

        Message sentMessage = absSender.execute(answer);
        messageCleaner.cleanAfterDelay(absSender, sentMessage);
    }

    private boolean isSomeDayRetry(UserId userId, UserId replyUserId) {
        Optional<TradeHistory> lastTradeHistory =
                tradeHistoryRepository.findByParticipantsUserIdInOrderByTimestampDesc(userId).stream()
                        .filter(tradeHistory ->
                                tradeHistory.getParticipants().stream().
                                        map(Participant::getUserId)
                                        .collect(Collectors.toSet())
                                        .containsAll(List.of(userId, replyUserId))
                        )
                        .findFirst();

        if (lastTradeHistory.isEmpty()) return false;

        List<Participant> participants = lastTradeHistory.get().getParticipants();
        if (!participants.get(0).isApproveTrade() || !participants.get(1).isApproveTrade()) return false;

        Instant lastTimestamp = lastTradeHistory.get().getTimestamp();

        return lastTimestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                .isEqual(LocalDate.now());
    }

    private boolean isApproveFromSecondSide(UserId userId, UserId replyUserId) {
        Optional<TradeHistory> lastTradeHistory =
                tradeHistoryRepository.findByParticipantsUserIdInOrderByTimestampDesc(userId).stream()
                        .filter(tradeHistory ->
                                tradeHistory.getParticipants().stream().
                                        map(Participant::getUserId)
                                        .collect(Collectors.toSet())
                                        .containsAll(List.of(userId, replyUserId))
                        )
                        .findFirst();

        if (lastTradeHistory.isEmpty()) return false;

        Participant sameParticipant = lastTradeHistory.get().getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(userId))
                .findFirst()
                .get();

        Participant another = lastTradeHistory.get().getParticipants().stream()
                .filter(participant -> !participant.getUserId().equals(userId))
                .findFirst()
                .get();

        return !sameParticipant.isApproveTrade() && another.isApproveTrade();
    }
}
