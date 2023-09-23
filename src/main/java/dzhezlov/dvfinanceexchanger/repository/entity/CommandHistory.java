package dzhezlov.dvfinanceexchanger.repository.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
public class CommandHistory {
    @Id
    private String id;
    @Indexed
    private UserId userId;
    private String command;
    @Indexed(expireAfter = "7d")
    private Instant timestamp;
}