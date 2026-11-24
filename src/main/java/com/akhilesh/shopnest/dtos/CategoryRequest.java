package com.akhilesh.shopnest.dtos;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name, String description) {
}
