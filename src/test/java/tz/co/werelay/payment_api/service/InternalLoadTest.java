package tz.co.werelay.payment_api.service;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.repository.PaymentRepository;

@SpringBootTest
public class InternalLoadTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void internalLoadTest() throws InterruptedException {
        int totalRequests = 500;
        int concurrencyLevel = 50;

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    PaymentRequest req = new PaymentRequest();
                    req.setClientRequestId(UUID.randomUUID().toString());
                    req.setFromAccount("ACC" + ThreadLocalRandom.current().nextInt(1, 100));
                    req.setToAccount("ACC" + ThreadLocalRandom.current().nextInt(101, 200));
                    req.setAmount(ThreadLocalRandom.current().nextDouble(10, 1000));

                    paymentService.process(req);

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("Load test completed.");
    }
}
