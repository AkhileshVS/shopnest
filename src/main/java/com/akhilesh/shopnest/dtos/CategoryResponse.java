package com.akhilesh.shopnest.dtos;

import com.akhilesh.shopnest.models.Category;

import java.io.Serializable;

public record CategoryResponse(Long id, String name, String description) implements Serializable {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
