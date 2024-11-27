package dzhezlov.dvfinanceexchanger.command;

import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.isAdminMessage;
import static dzhezlov.dvfinanceexchanger.command.utils.CommandUtils.toUserId;
import static dzhezlov.dvfinanceexchanger.command.utils.FormatUtils.toMention;
import static java.util.stream.Collectors.mapping;

@Component
@RequiredArgsConstructor
public class StatsCommand implements IBotCommand {

    private final TradeHistoryRepository tradeHistoryRepository;
    private final MessageCleaner messageCleaner;

    @Override
    public String getCommandIdentifier() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "stats";
    }

    @SneakyThrows
    @Override
    public void processMessage(AbsSender absSender, Message message, String[] strings) {
        if (message.isSuperGroupMessage() && isAdminMessage(message, absSender)) {
            Map<Participant, List<TradeHistory>> allHistory = tradeHistoryRepository.findAllByParticipantsUserIdIn(toUserId(message)).stream()
                    .filter(tradeHistory -> tradeHistory.getMessageId().getChatId().equals(message.getChatId()))
                    .filter(tradeHistory ->
                            tradeHistory.getParticipants().stream()
                                    .allMatch(Participant::isApproveTrade)
                    )
                    .flatMap(tradeHistory ->
                            tradeHistory.getParticipants().stream()
                                    .map(participant -> new AbstractMap.SimpleEntry<>(participant, tradeHistory))
                    )
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            mapping(Map.Entry::getValue, Collectors.toList())
                    ));

            List<Participant> topParticipants = allHistory.entrySet().stream()
                    .sorted((entry1, entry2) -> Long.compare(entry2.getValue().size(), entry1.getValue().size()))
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (topParticipants.isEmpty()) {
                return;
            }

            StringBuilder result = new StringBuilder();

            for (Participant participant : topParticipants) {
                List<TradeHistory> tradeHistories = allHistory.get(participant);
                long uniqueTrades = tradeHistories.stream()
                        .flatMap(tradeHistory -> tradeHistory.getParticipants().stream())
                        .filter(tradeParticipant -> !tradeParticipant.getUserId().equals(participant.getUserId()))
                        .distinct()
                        .count();

                result
                        .append("- ")
                        .append(toMention(participant))
                        .append("\n")
                        .append(" Обменов - ")
                        .append(tradeHistories.size())
                        .append(". Уникальных обменов - ")
                        .append(uniqueTrades)
                        .append("\n");
            }

            SendMessage answer = new SendMessage();
            answer.setChatId(message.getChatId());
            answer.setReplyToMessageId(message.getMessageId());
            answer.enableHtml(true);
            answer.setText(result.toString());

            Message sentMessage = absSender.execute(answer);
            messageCleaner.cleanAfterDelay(absSender, sentMessage, 300);
        }

        messageCleaner.cleanAfterDelay(absSender, message, 3);
    }
}
