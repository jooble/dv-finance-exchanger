package dzhezlov.dvfinanceexchanger;

import dzhezlov.dvfinanceexchanger.config.TradeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableConfigurationProperties(TradeProperties.class)
@EnableMongoRepositories
public class DvFinanceExchangerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DvFinanceExchangerApplication.class, args);
    }

}
