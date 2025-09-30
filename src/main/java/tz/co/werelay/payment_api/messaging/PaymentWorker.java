package tz.co.werelay.payment_api.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.model.PaymentResponse;
import tz.co.werelay.payment_api.service.PaymentService;

@Component
public class PaymentWorker {

    @Autowired
    private PaymentService paymentService;

    @RabbitListener(queues = RabbitConfig.PAYMENT_REQUEST_QUEUE)
    public PaymentResponse handlePayment(PaymentRequest request) {
        // delegate to PaymentService to centralize idempotency and ledger writes
        return paymentService.process(request);
    }
}