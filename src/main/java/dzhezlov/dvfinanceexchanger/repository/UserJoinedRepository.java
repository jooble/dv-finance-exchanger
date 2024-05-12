package dzhezlov.dvfinanceexchanger.repository;

import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import dzhezlov.dvfinanceexchanger.repository.entity.UserJoined;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserJoinedRepository extends MongoRepository<UserJoined, UserId> {
}
