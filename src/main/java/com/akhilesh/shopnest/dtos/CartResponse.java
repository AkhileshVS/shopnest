package com.akhilesh.shopnest.dtos;

import com.akhilesh.shopnest.models.Cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long cartId, List<CartItemResponse> items, BigDecimal total) {

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(i -> new CartItemResponse(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getPrice(),
                        i.getQuantity(),
                        i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
