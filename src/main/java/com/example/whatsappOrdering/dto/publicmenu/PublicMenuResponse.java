package com.example.whatsappOrdering.dto.publicmenu;

import java.math.BigDecimal;
import java.util.List;

public record PublicMenuResponse(
        Long restaurantId,
        String restaurantName,
        String tableNumber,
        List<CategorySection> categories
) {
    public record CategorySection(
            Long id,
            String name,
            int displayOrder,
            List<ItemSummary> items
    ) {}

    public record ItemSummary(
            Long id,
            String name,
            String description,
            BigDecimal price,
            String imageUrl
    ) {}
}
