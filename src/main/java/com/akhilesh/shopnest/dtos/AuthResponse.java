package com.akhilesh.shopnest.dtos;

public record AuthResponse(String token, Long userId, String email, String role) {
}
