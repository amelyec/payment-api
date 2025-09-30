package tz.co.werelay.payment_api.messaging;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String PAYMENT_REQUEST_QUEUE = "payment_requests";

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_REQUEST_QUEUE, true);
    }
}