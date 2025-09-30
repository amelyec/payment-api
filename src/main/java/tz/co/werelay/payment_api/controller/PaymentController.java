package tz.co.werelay.payment_api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tz.co.werelay.payment_api.model.PaymentRequest;
import tz.co.werelay.payment_api.model.PaymentResponse;
import tz.co.werelay.payment_api.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public PaymentResponse makePayment(@RequestBody PaymentRequest request) {
        return paymentService.process(request);
    }
}
