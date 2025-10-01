package tz.co.werelay.payment_api.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tz.co.werelay.payment_api.messaging.RabbitConfig;
import tz.co.werelay.payment_api.model.PaymentRequest;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping
    public ResponseEntity<?> makePayment(@RequestBody PaymentRequest request) {
        if (request.getClientRequestId() == null) {
            request.setClientRequestId(UUID.randomUUID().toString());
        }
        log.info("Enqueuing payment request to RabbitMQ: {}", request);
        rabbitTemplate.convertAndSend(RabbitConfig.PAYMENT_REQUEST_QUEUE, request);

        return ResponseEntity.accepted()
                .body(java.util.Map.of(
                        "success", true,
                        "clientRequestId", request.getClientRequestId(),
                        "message", "Payment request queued"));
    }
}
