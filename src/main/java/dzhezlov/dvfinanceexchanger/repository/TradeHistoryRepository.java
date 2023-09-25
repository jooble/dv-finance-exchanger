package dzhezlov.dvfinanceexchanger.repository;

import dzhezlov.dvfinanceexchanger.repository.entity.Participant;
import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {

    List<TradeHistory> findByParticipantsUserIdIn(UserId recipient);

    List<TradeHistory> findByParticipantsUserIdInOrderByTimestampDesc(UserId userId);
}
