package dzhezlov.dvfinanceexchanger.command;

import lombok.SneakyThrows;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

public interface CallbackCommand {

    boolean isCanProcess(AbsSender absSender, Update message);

    @SneakyThrows
    void processCallback(AbsSender absSender, Update message);
}
