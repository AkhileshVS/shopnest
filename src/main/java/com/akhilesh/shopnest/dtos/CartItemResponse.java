package com.akhilesh.shopnest.dtos;

import java.math.BigDecimal;

public record CartItemResponse(Long productId, String productName, BigDecimal price, int quantity, BigDecimal subtotal) {
}
