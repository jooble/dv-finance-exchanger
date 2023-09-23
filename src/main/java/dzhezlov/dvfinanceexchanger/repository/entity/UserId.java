package dzhezlov.dvfinanceexchanger.repository.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class UserId {
    private long chatId;
    private long userId;
}
