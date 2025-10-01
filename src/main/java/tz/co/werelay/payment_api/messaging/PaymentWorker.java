package tz.co.werelay.payment_api.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.model.PaymentResponse;
import tz.co.werelay.payment_api.service.PaymentService;

@Component
public class PaymentWorker {

    private static final Logger log = LoggerFactory.getLogger(PaymentWorker.class);
    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = RabbitConfig.PAYMENT_REQUEST_QUEUE)
    public void handlePayment(PaymentRequest request) {
            log.info("Consuming payment request from RabbitMQ: {}", request);
        // delegate to PaymentService to centralize idempotency and ledger writes
        paymentService.process(request);
    }
}