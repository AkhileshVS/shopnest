package com.akhilesh.shopnest.services.payment;

public record WebhookResult(String referenceId, String gatewayPaymentId, boolean success) {
}
