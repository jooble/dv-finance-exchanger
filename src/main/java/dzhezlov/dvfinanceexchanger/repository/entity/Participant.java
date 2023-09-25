package dzhezlov.dvfinanceexchanger.repository.entity;


import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class Participant {
    private UserId userId;
    private boolean approveTrade;
}
