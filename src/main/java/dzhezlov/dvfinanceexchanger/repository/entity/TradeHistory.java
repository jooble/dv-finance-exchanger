package dzhezlov.dvfinanceexchanger.repository.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class TradeHistory {
    @Id
    private String id;
    @Indexed
    private UserId tradeInitiator;
    private UserId doneSender;
    @Indexed
    private List<UserId> participants;
    private Instant timestamp;
}
