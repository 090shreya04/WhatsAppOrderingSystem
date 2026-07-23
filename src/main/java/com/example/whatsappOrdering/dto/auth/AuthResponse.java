package com.example.whatsappOrdering.dto.auth;

public record AuthResponse(
        String token,
        long expiresIn,
        Long userId,
        String name,
        String email
) {}
