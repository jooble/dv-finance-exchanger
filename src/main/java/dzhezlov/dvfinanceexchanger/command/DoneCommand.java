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
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.*;
import static dzhezlov.dvfinanceexchanger.command.utils.FormatUtils.toMention;

@Component
@RequiredArgsConstructor
public class DoneCommand implements IBotCommand, CallbackCommand {

    private static final String COMMAND = "done";
    private static final String CALLBACK_LINK = COMMAND + "/";
    private static final String CALLBACK_APPROVE = CALLBACK_LINK + "approve";
    private static final String CALLBACK_REFUSE = CALLBACK_LINK + "refuse";

    private final TradeHistoryRepository tradeHistoryRepository;
    private final MessageCleaner messageCleaner;

    @Override
    public String getCommandIdentifier() {
        return COMMAND;
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
            } else {
                processFirstApprove(absSender, message, userId, replyUserId);
            }
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }

    @Override
    public boolean isCanProcess(AbsSender absSender, Update update) {
        return update.getCallbackQuery().getData().startsWith(CALLBACK_LINK);
    }

    @SneakyThrows
    @Override
    public void processCallback(AbsSender absSender, Update update) {
        Message message = update.getCallbackQuery().getMessage();
        UserId clickedUserId = UserId.builder()
                .userId(update.getCallbackQuery().getFrom().getId())
                .chatId(message.getChatId())
                .build();
        String callBackAnswer = update.getCallbackQuery().getData();

        Optional<TradeHistory> tradeHistoryOptional = tradeHistoryRepository.findByMessageId(MessageId.builder()
                .messageId(message.getMessageId())
                .chatId(message.getChatId())
                .build());

        if (tradeHistoryOptional.isEmpty()) return;

        TradeHistory tradeHistory = tradeHistoryOptional.get();
        Optional<Participant> clickedParticipant = tradeHistory.getParticipants().stream()
                .filter(participant -> participant.getUserId().equals(clickedUserId))
                .findFirst();

        if (clickedParticipant.isPresent() && !clickedParticipant.get().isApproveTrade()) {
            if (CALLBACK_APPROVE.equals(callBackAnswer)) {
                Participant sameParticipant = tradeHistory.getParticipants().stream()
                        .filter(participant -> participant.getUserId().equals(clickedUserId))
                        .findFirst()
                        .get();

                sameParticipant.setApproveTrade(true);
                tradeHistory.setExpireTime(null);

                tradeHistoryRepository.save(tradeHistory);

                StringBuilder answerText = new StringBuilder()
                        .append("Спасибо, произведен обмен между ")
                        .append(toMention(update.getCallbackQuery().getFrom()))
                        .append(" и ")
                        .append(toMention(message.getFrom()));
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.enableHtml(true);
                answer.setText(answerText.toString());

                absSender.execute(answer);

                messageCleaner.cleanAfterDelay(absSender, message, 3);
            }
            if (CALLBACK_REFUSE.equals(callBackAnswer)) {
                tradeHistoryRepository.delete(tradeHistory);

                StringBuilder answerText = new StringBuilder()
                        .append("Обмен между ")
                        .append(toMention(update.getCallbackQuery().getFrom()))
                        .append(" и ")
                        .append(toMention(message.getFrom()))
                        .append(" отклонен");
                SendMessage answer = new SendMessage();
                answer.setChatId(message.getChatId());
                answer.enableHtml(true);
                answer.setText(answerText.toString());

                Message sentMessage = absSender.execute(answer);

                messageCleaner.cleanAfterDelay(absSender, sentMessage);
                messageCleaner.cleanAfterDelay(absSender, message, 3);
            }
        }
    }

    private void processFirstApprove(AbsSender absSender, Message message, UserId userId, UserId replyUserId) throws TelegramApiException {
        StringBuilder answerText = new StringBuilder()
                .append(toMention(message.getFrom()))
                .append(" просит подтвердить обмен с ")
                .append(toMention(message.getReplyToMessage().getFrom()))
                .append(" подтвердите/отклоните ");
        SendMessage answer = new SendMessage();
        answer.setChatId(message.getChatId());
        answer.enableHtml(true);
        answer.setText(answerText.toString());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> buttons = new ArrayList<>();

        InlineKeyboardButton approveButton = new InlineKeyboardButton("Да");
        approveButton.setCallbackData(CALLBACK_APPROVE);
        InlineKeyboardButton refuseButton = new InlineKeyboardButton("Нет");
        refuseButton.setCallbackData(CALLBACK_REFUSE);

        buttons.add(approveButton);
        buttons.add(refuseButton);
        keyboard.add(buttons);
        inlineKeyboardMarkup.setKeyboard(keyboard);
        answer.setReplyMarkup(inlineKeyboardMarkup);

        Message sentMessage = absSender.execute(answer);

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
                        .messageId(MessageId.builder()
                                .messageId(sentMessage.getMessageId())
                                .chatId(sentMessage.getChatId())
                                .build())
                        .timestamp(Instant.now())
                        .expireTime(LocalDateTime.now().plusDays(7).toInstant(OffsetDateTime.now().getOffset()))
                        .build()
        );
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
}
