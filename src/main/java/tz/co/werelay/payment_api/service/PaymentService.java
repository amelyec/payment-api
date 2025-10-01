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
    public PaymentRepository paymentRepository;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.duplicate-threshold-minutes}")
    public long duplicateThresholdMinutes;

    @Transactional
    public PaymentResponse process(PaymentRequest req) {
            log.info("Start processing payment: clientRequestId={}, from={}, to={}, amount={}", req.getClientRequestId(), req.getFromAccount(), req.getToAccount(), req.getAmount());
        // 1) Exact idempotency by clientRequestId
        var existing = paymentRepository.findByClientRequestId(req.getClientRequestId());
        if (existing.isPresent()) {
            PaymentEntity e = existing.get();
                log.info("Idempotent request detected for clientRequestId={}, returning existing transactionId={}", req.getClientRequestId(), e.getTransactionId());
            return new PaymentResponse("SUCCESS".equals(e.getStatus()), e.getTransactionId(), null);
        }

        // 2) Semantic duplicate check
        OffsetDateTime thresholdTime = OffsetDateTime.now().minusMinutes(duplicateThresholdMinutes);
        List<PaymentEntity> possibleDupes = paymentRepository.findRecentDuplicate(
                req.getFromAccount(),
                req.getToAccount(),
                req.getAmount(),
                thresholdTime);

        if (!possibleDupes.isEmpty()) {
            PaymentEntity e = possibleDupes.get(0);
                log.info("Duplicate payment detected within threshold for from={}, to={}, amount={}", req.getFromAccount(), req.getToAccount(), req.getAmount());
            return new PaymentResponse("SUCCESS".equals(e.getStatus()), e.getTransactionId(),
                    "DUPLICATE_WITHIN_THRESHOLD");
        }

        // 3) Proceed with new payment
        PaymentEntity entity = new PaymentEntity();
        entity.setClientRequestId(req.getClientRequestId());
        entity.setFromAccount(req.getFromAccount());
        entity.setToAccount(req.getToAccount());
        entity.setAmount(req.getAmount());
        entity.setStatus("PENDING");
        entity.setCreatedAt(OffsetDateTime.now());

        paymentRepository.save(entity);
            log.info("Payment saved as PENDING, clientRequestId={}", req.getClientRequestId());

        // Simulate charge success
        String txId = "tx-" + UUID.randomUUID();
        entity.setTransactionId(txId);
        entity.setStatus("SUCCESS");
        paymentRepository.save(entity);
            log.info("Payment processed successfully, transactionId={}", txId);

        // Publish to Kafka after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                    log.info("Publishing payment_completed event to Kafka for transactionId={}", txId);
                var event = new java.util.HashMap<String, Object>();
                event.put("eventType", "payment_completed");
                event.put("payload", java.util.Map.of(
                        "clientRequestId", req.getClientRequestId(),
                        "transactionId", txId,
                        "amount", req.getAmount()));
                kafkaTemplate.send("payments", req.getClientRequestId(), event);
            }
        });

        return new PaymentResponse(true, txId, null);
    }
}