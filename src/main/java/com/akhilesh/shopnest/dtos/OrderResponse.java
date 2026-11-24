package com.akhilesh.shopnest.dtos;

import com.akhilesh.shopnest.models.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String status,
        BigDecimal totalAmount,
        String shippingAddress,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getPriceAtPurchase()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount(),
                order.getShippingAddress(), order.getCreatedAt(), items);
    }
}
