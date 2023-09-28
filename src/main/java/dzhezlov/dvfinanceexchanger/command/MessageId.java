package dzhezlov.dvfinanceexchanger.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class MessageId {
    Integer messageId;
    Long chatId;
}
