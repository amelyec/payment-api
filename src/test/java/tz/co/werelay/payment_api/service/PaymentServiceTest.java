package tz.co.werelay.payment_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import tz.co.werelay.payment_api.entity.PaymentEntity;
import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.model.PaymentResponse;
import tz.co.werelay.payment_api.repository.PaymentRepository;
// ...existing code...

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class PaymentServiceTest {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    // ...existing code...

    @Test
    void shouldReturnExistingPaymentForIdempotency() {
        PaymentEntity existing = new PaymentEntity();
        existing.setClientRequestId("abc123");
        existing.setTransactionId("tx-111");
        existing.setStatus("SUCCESS");
        existing.setAmount(100.0);
        existing.setFromAccount("A1");
        existing.setToAccount("B1");
        existing.setCreatedAt(java.time.OffsetDateTime.now());
        paymentRepository.save(existing);

        PaymentRequest req = new PaymentRequest();
        req.setClientRequestId("abc123");
        req.setFromAccount("A1");
        req.setToAccount("B1");
        req.setAmount(100.0);

        PaymentResponse response = paymentService.process(req);

        assertThat(response.getTransactionId()).isEqualTo("tx-111");
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void shouldProcessNewPayment() {
        PaymentRequest req = new PaymentRequest();
        req.setClientRequestId(UUID.randomUUID().toString());
        req.setFromAccount("ACC1");
        req.setToAccount("ACC2");
        req.setAmount(50.0);

        PaymentResponse response = paymentService.process(req);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTransactionId()).isNotBlank();
        // Optionally, verify the payment is saved in the repository
        var saved = paymentRepository.findByClientRequestId(req.getClientRequestId());
        assertThat(saved).isPresent();
    }
}
