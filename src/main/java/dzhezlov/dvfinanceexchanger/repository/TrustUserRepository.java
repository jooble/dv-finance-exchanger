package dzhezlov.dvfinanceexchanger.repository;

import dzhezlov.dvfinanceexchanger.repository.entity.TrustUser;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrustUserRepository extends MongoRepository<TrustUser, UserId> {
}
