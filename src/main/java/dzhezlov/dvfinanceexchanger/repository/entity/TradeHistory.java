package dzhezlov.dvfinanceexchanger.repository.entity;

import dzhezlov.dvfinanceexchanger.command.MessageId;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder(toBuilder = true)
@Document(collection = "tradeHistories")
public class TradeHistory {
    @Id
    private String id;
    @Indexed
    private List<Participant> participants;
    private Instant timestamp;
    @Indexed
    private MessageId messageId;
    @Indexed(expireAfterSeconds = 0)
    private Instant expireTime;
}
