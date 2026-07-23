package com.example.whatsappOrdering.dto.menu;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available,
        Long categoryId,
        String categoryName
) {}
