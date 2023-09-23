package dzhezlov.dvfinanceexchanger.repository;

import dzhezlov.dvfinanceexchanger.repository.entity.CommandHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommandHistoryRepository extends MongoRepository<CommandHistory, String> {

    Optional<CommandHistory> findFirstByUserIdAndCommandOrderByTimestampDesc(UserId userId, String command);
}
