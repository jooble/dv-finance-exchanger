package dzhezlov.dvfinanceexchanger.repository;

import dzhezlov.dvfinanceexchanger.repository.entity.TradeHistory;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeHistoryRepository extends MongoRepository<TradeHistory, String> {

    List<TradeHistory> findByParticipantsIn(UserId recipient);

    List<TradeHistory> findByTradeInitiator(UserId tradeInitiator);
}
