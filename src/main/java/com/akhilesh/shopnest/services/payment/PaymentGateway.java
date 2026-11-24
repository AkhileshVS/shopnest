package com.akhilesh.shopnest.services.payment;

import com.akhilesh.shopnest.models.Payment;
import com.akhilesh.shopnest.models.PaymentGatewayType;
import com.akhilesh.shopnest.models.User;

import java.util.Optional;

public interface PaymentGateway {

    PaymentGatewayType type();

    String createPaymentLink(Payment payment, User customer);

    Optional<WebhookResult> parseWebhook(String payload, String signature);
}
