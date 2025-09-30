package tz.co.werelay.payment_api.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic paymentsTopic() {
        return new NewTopic("payments", 1, (short) 1);
    }
}