package tz.co.werelay.payment_api.service;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class LoadPaymentServiceTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String endpoint = "http://localhost:8080/payments";

    @Test
    void runLoadTest() throws InterruptedException {
        int totalRequests = 500; // adjust between 100 and 500
        int concurrencyLevel = 50; // number of concurrent threads

        ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        IntStream.range(0, totalRequests).forEach(i -> executor.submit(() -> {
            try {
                PaymentRequestBody requestBody = new PaymentRequestBody(
                        UUID.randomUUID().toString(),
                        "ACC" + ThreadLocalRandom.current().nextInt(1, 100),
                        "ACC" + ThreadLocalRandom.current().nextInt(101, 200),
                        ThreadLocalRandom.current().nextDouble(10.0, 1000.0));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<PaymentRequestBody> request = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

                System.out.println("Status: " + response.getStatusCode() + ", Response: " + response.getBody());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }));

        latch.await(); // wait until all requests complete
        executor.shutdown();
    }

    static class PaymentRequestBody {
        private String clientRequestId;
        private String fromAccount;
        private String toAccount;
        private double amount;

        public PaymentRequestBody(String clientRequestId, String fromAccount, String toAccount, double amount) {
            this.clientRequestId = clientRequestId;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.amount = amount;
        }

        public String getClientRequestId() {
            return clientRequestId;
        }

        public String getFromAccount() {
            return fromAccount;
        }

        public String getToAccount() {
            return toAccount;
        }

        public double getAmount() {
            return amount;
        }
    }
}
