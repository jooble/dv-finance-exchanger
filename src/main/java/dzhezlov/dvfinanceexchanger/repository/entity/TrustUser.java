package dzhezlov.dvfinanceexchanger.repository.entity;


import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder(toBuilder = true)
@Document(collection = "trustUsers")
public class TrustUser {

    @Id
    private UserId userId;
    private UserId sender;
}
