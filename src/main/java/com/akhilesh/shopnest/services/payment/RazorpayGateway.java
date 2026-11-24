package com.akhilesh.shopnest.services.payment;

import com.akhilesh.shopnest.exceptions.PaymentException;
import com.akhilesh.shopnest.models.Payment;
import com.akhilesh.shopnest.models.PaymentGatewayType;
import com.akhilesh.shopnest.models.User;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class RazorpayGateway implements PaymentGateway {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String callbackUrl;

    public RazorpayGateway(@Value("${app.payment.razorpay.key-id}") String keyId,
                           @Value("${app.payment.razorpay.key-secret}") String keySecret,
                           @Value("${app.payment.razorpay.webhook-secret}") String webhookSecret,
                           @Value("${app.payment.callback-url}") String callbackUrl) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public PaymentGatewayType type() {
        return PaymentGatewayType.RAZORPAY;
    }

    @Override
    public String createPaymentLink(Payment payment, User customer) {
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", toPaise(payment.getAmount()));
            request.put("currency", "INR");
            request.put("reference_id", payment.getReferenceId());
            request.put("description", "ShopNest order #" + payment.getOrder().getId());
            request.put("expire_by", Instant.now().plus(30, ChronoUnit.MINUTES).getEpochSecond());
            request.put("callback_url", callbackUrl);
            request.put("callback_method", "get");

            JSONObject cust = new JSONObject();
            cust.put("name", customer.getName());
            cust.put("email", customer.getEmail());
            if (customer.getPhone() != null) {
                cust.put("contact", customer.getPhone());
            }
            request.put("customer", cust);

            JSONObject notify = new JSONObject();
            notify.put("email", true);
            request.put("notify", notify);

            PaymentLink link = client.paymentLink.create(request);
            return link.get("short_url").toString();
        } catch (RazorpayException e) {
            throw new PaymentException("Could not create Razorpay payment link", e);
        }
    }

    @Override
    public Optional<WebhookResult> parseWebhook(String payload, String signature) {
        try {
            if (signature == null || !Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                throw new PaymentException("Invalid Razorpay signature");
            }
        } catch (RazorpayException e) {
            throw new PaymentException("Invalid Razorpay signature", e);
        }

        JSONObject body = new JSONObject(payload);
        String event = body.getString("event");
        JSONObject data = body.getJSONObject("payload");

        if ("payment_link.paid".equals(event)) {
            String reference = data.getJSONObject("payment_link").getJSONObject("entity").getString("reference_id");
            String paymentId = data.getJSONObject("payment").getJSONObject("entity").getString("id");
            return Optional.of(new WebhookResult(reference, paymentId, true));
        }
        if ("payment_link.expired".equals(event) || "payment_link.cancelled".equals(event)) {
            String reference = data.getJSONObject("payment_link").getJSONObject("entity").getString("reference_id");
            return Optional.of(new WebhookResult(reference, null, false));
        }
        return Optional.empty();
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }
}
