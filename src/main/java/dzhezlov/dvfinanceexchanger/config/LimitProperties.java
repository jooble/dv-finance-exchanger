package dzhezlov.dvfinanceexchanger.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("limit")
@Data
public class LimitProperties {

    private Duration trade;
}
