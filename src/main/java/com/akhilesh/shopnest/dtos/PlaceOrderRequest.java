package com.akhilesh.shopnest.dtos;

import com.akhilesh.shopnest.models.PaymentGatewayType;
import jakarta.validation.constraints.NotBlank;

public record PlaceOrderRequest(@NotBlank String shippingAddress, PaymentGatewayType gateway) {
}
