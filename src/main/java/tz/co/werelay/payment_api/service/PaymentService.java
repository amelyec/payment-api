package tz.co.werelay.payment_api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tz.co.werelay.payment_api.entity.PaymentEntity;
import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.model.PaymentResponse;
import tz.co.werelay.payment_api.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.duplicate-threshold-minutes:5}")
    private long duplicateThresholdMinutes;

    @Transactional
    public PaymentResponse process(PaymentRequest req) {
        log.info("Start processing payment: {}", req);

        // Idempotency check
        var existing = paymentRepository.findByClientRequestId(req.getClientRequestId());
        if (existing.isPresent()) {
            PaymentEntity e = existing.get();
            return new PaymentResponse("SUCCESS".equals(e.getStatus()), e.getTransactionId(), "IDEMPOTENT");
        }

        // Duplicate detection
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(duplicateThresholdMinutes);
        var dupes = paymentRepository.findRecentDuplicate(req.getFromAccount(), req.getToAccount(), req.getAmount(), threshold);
        if (!dupes.isEmpty()) {
            PaymentEntity e = dupes.get(0);
            return new PaymentResponse(true, e.getTransactionId(), "DUPLICATE_WITHIN_THRESHOLD");
        }

        // Save pending
        PaymentEntity entity = new PaymentEntity();
        entity.setClientRequestId(req.getClientRequestId());
        entity.setFromAccount(req.getFromAccount());
        entity.setToAccount(req.getToAccount());
        entity.setAmount(req.getAmount());
        entity.setStatus("PENDING");
        entity.setCreatedAt(OffsetDateTime.now());
        paymentRepository.save(entity);

        // Simulate success
        String txId = "tx-" + UUID.randomUUID();
        entity.setTransactionId(txId);
        entity.setStatus("SUCCESS");
        paymentRepository.save(entity);

        // After commit publish to Kafka
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("Publishing event to Kafka: transactionId={}", txId);
                var event = java.util.Map.of(
                        "eventType", "payment_completed",
                        "transactionId", txId,
                        "clientRequestId", req.getClientRequestId(),
                        "amount", req.getAmount()
                );
                kafkaTemplate.send("payments", req.getClientRequestId(), event);
            }
        });

        return new PaymentResponse(true, txId, null);
    }
}
