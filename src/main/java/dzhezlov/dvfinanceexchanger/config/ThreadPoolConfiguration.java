package dzhezlov.dvfinanceexchanger.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ThreadPoolConfiguration {

    @Bean
    public ScheduledExecutorService messageCleanerPool() {
        return Executors.newScheduledThreadPool(1);
    }
}
