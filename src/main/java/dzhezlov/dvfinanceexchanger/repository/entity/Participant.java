package dzhezlov.dvfinanceexchanger.repository.entity;


import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
public class Participant {
    @EqualsAndHashCode.Include
    private UserId userId;
    private String fullName;
    private boolean approveTrade;
}
