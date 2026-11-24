package com.akhilesh.shopnest.dtos;

import java.math.BigDecimal;

public record CheckoutResponse(Long orderId, BigDecimal amount, String paymentReference, String paymentLink) {
}
