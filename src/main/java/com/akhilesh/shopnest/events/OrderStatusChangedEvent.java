package com.akhilesh.shopnest.events;

import com.akhilesh.shopnest.models.OrderStatus;

import java.math.BigDecimal;

public record OrderStatusChangedEvent(Long orderId, Long userId, String email, OrderStatus status, BigDecimal amount) {
}
