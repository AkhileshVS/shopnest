package com.akhilesh.shopnest.services.payment;

import com.akhilesh.shopnest.exceptions.PaymentException;
import com.akhilesh.shopnest.models.Payment;
import com.akhilesh.shopnest.models.PaymentGatewayType;
import com.akhilesh.shopnest.models.User;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class StripeGateway implements PaymentGateway {

    private final String apiKey;
    private final String webhookSecret;
    private final String callbackUrl;

    public StripeGateway(@Value("${app.payment.stripe.api-key}") String apiKey,
                         @Value("${app.payment.stripe.webhook-secret}") String webhookSecret,
                         @Value("${app.payment.callback-url}") String callbackUrl) {
        this.apiKey = apiKey;
        this.webhookSecret = webhookSecret;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.STRIPE;
    }

    @Override
    public String createPaymentLink(Payment payment, User customer) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(payment.getReferenceId())
                .setCustomerEmail(customer.getEmail())
                .setSuccessUrl(callbackUrl + "?status=success&ref=" + payment.getReferenceId())
                .setCancelUrl(callbackUrl + "?status=cancelled&ref=" + payment.getReferenceId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("inr")
                                .setUnitAmount(toPaise(payment.getAmount()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("ShopNest order #" + payment.getOrder().getId())
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params, RequestOptions.builder().setApiKey(apiKey).build());
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentException("Could not create Stripe checkout session", e);
        }
    }

    @Override
    public Optional<WebhookResult> parseWebhook(String payload, String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new PaymentException("Invalid Stripe signature", e);
        }

        Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
        if (obj.isEmpty() || !(obj.get() instanceof Session session)) {
            return Optional.empty();
        }

        return switch (event.getType()) {
            case "checkout.session.completed" ->
                    Optional.of(new WebhookResult(session.getClientReferenceId(), session.getPaymentIntent(), true));
            case "checkout.session.expired" ->
                    Optional.of(new WebhookResult(session.getClientReferenceId(), null, false));
            default -> Optional.empty();
        };
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
