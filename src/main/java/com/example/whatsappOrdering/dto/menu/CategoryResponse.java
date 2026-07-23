package com.example.whatsappOrdering.dto.menu;

public record CategoryResponse(
        Long id,
        String name,
        int displayOrder
) {}
