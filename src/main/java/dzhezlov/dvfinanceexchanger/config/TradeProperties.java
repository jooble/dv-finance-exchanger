package dzhezlov.dvfinanceexchanger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("trade")
@Data
public class TradeProperties {

    private Duration firstTimeout;
    private Duration Timeout;
}
