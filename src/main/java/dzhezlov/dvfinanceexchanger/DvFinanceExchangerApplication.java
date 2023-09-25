package dzhezlov.dvfinanceexchanger;

import dzhezlov.dvfinanceexchanger.config.TradeProperties;
import dzhezlov.dvfinanceexchanger.repository.TradeHistoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableConfigurationProperties(TradeProperties.class)
@EnableMongoRepositories
public class DvFinanceExchangerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DvFinanceExchangerApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            try {
                TradeHistoryRepository bean = ctx.getBean(TradeHistoryRepository.class);
                bean.deleteAll();
            } catch (Exception e) {

            }
        };
    }

}
