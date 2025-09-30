package tz.co.werelay.payment_api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tz.co.werelay.payment_api.entity.PaymentEntity;
import tz.co.werelay.payment_api.repository.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldCreatePaymentAndSaveInDb() throws Exception {
    paymentRepository.deleteAll(); // Ensure clean state
    String clientRequestId = UUID.randomUUID().toString();

    mockMvc.perform(post("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"clientRequestId\":\"" + clientRequestId
            + "\",\"fromAccount\":\"X1\",\"toAccount\":\"Y1\",\"amount\":150.0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.note").doesNotExist());

    PaymentEntity saved = paymentRepository.findByClientRequestId(clientRequestId).orElse(null);

    assertThat(saved).isNotNull();
    assertThat(saved.getFromAccount()).isEqualTo("X1");
    assertThat(saved.getAmount()).isEqualTo(150.0);
    }
}
