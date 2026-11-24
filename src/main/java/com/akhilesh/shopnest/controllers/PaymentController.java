package com.akhilesh.shopnest.controllers;

import com.akhilesh.shopnest.models.PaymentGatewayType;
import com.akhilesh.shopnest.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Void> razorpayWebhook(@RequestBody String payload,
                                                @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(PaymentGatewayType.RAZORPAY, payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(@RequestBody String payload,
                                              @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.handleWebhook(PaymentGatewayType.STRIPE, payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/callback")
    public Map<String, String> callback(@RequestParam Map<String, String> params) {
        return Map.of("message", "Payment is being processed. Check your order status for the final result.");
    }
}
