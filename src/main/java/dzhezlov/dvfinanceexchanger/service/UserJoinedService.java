package dzhezlov.dvfinanceexchanger.service;

import dzhezlov.dvfinanceexchanger.config.TradeProperties;
import dzhezlov.dvfinanceexchanger.repository.UserJoinedRepository;
import dzhezlov.dvfinanceexchanger.repository.entity.UserId;
import dzhezlov.dvfinanceexchanger.repository.entity.UserJoined;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserJoinedService {

    private final UserJoinedRepository userJoinedRepository;
    private final TradeProperties tradeProperties;

    public void processMessage(Message message) {
        if (!message.getNewChatMembers().isEmpty()) {
            List<UserJoined> joinedUsers = message.getNewChatMembers().stream()
                    .map(user -> UserJoined.builder()
                            .userId(toUserId(user, message))
                            .timestamp(Instant.now())
                            .build()
                    )
                    .collect(Collectors.toList());

            userJoinedRepository.saveAll(joinedUsers);
        }
    }

    public Instant getDataJoined(UserId userId) {
        Optional<UserJoined> userJoined = userJoinedRepository.findById(userId);
        if (userJoined.isPresent()) {
            return userJoined.get().getTimestamp();
        } else {
            Instant minus = Instant.now().minus(tradeProperties.getFirstTimeout()).minus(1, ChronoUnit.DAYS);

            userJoinedRepository.save(
                    UserJoined.builder()
                            .userId(userId)
                            .timestamp(minus)
                            .build());

            return minus;
        }
    }

    private UserId toUserId(User user, Message message) {
        return UserId.builder()
                .userId(user.getId())
                .chatId(message.getChatId())
                .build();
    }
}
