package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.TrustUserRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.toUserId;

@Component
@RequiredArgsConstructor
public class CheckCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final TrustUserRepository trustUserRepository;
    private final MessageCleaner messageCleaner;

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
        if (message.isReply()) {
            UserId recipient = toUserId(message.getReplyToMessage());
            List<TradeHistory> tradeHistories = tradeHistoryRepository.findByParticipantsIn(recipient);
            int countTrades = tradeHistories.size();
            long uniqueSenders = tradeHistories.stream()
                    .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                    .filter(userId -> !(userId.equals(recipient)))
                    .distinct()
                    .count();

            StringBuilder answerText = new StringBuilder()
                    .append("Обменов: ")
                    .append(countTrades)
                    .append("\nС участниками: ")
                    .append(uniqueSenders);
            trustUserRepository.findById(recipient)
                    .ifPresent(user -> answerText.append("\n ✅ Активный участник"));

            SendMessage answer = new SendMessage();
            answer.setChatId(message.getChatId());
            answer.setReplyToMessageId(message.getMessageId());
            answer.setText(answerText.toString());

            Message sentMessage = absSender.execute(answer);
            messageCleaner.cleanAfterDelay(absSender, sentMessage);
            messageCleaner.cleanAfterDelay(absSender, message);
        }
    }
}
