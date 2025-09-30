package tz.co.werelay.payment_api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public PaymentRepository paymentRepository;

    @Autowired
    public KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.duplicate-threshold-minutes}")
    public long duplicateThresholdMinutes;

    @Transactional
    public PaymentResponse process(PaymentRequest req) {
        // 1) Exact idempotency by clientRequestId
        var existing = paymentRepository.findByClientRequestId(req.getClientRequestId());
        if (existing.isPresent()) {
            PaymentEntity e = existing.get();
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

        // Simulate charge success
        String txId = "tx-" + UUID.randomUUID();
        entity.setTransactionId(txId);
        entity.setStatus("SUCCESS");
        paymentRepository.save(entity);

        // Publish to Kafka after commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
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