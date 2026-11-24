package com.akhilesh.shopnest.dtos;

import com.akhilesh.shopnest.models.Product;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        Long categoryId,
        String categoryName) implements Serializable {

    public static ProductResponse from(Product p) {
        Long categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStock(), p.getImageUrl(), categoryId, categoryName);
    }
}
