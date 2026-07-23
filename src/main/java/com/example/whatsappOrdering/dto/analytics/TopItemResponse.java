package com.example.whatsappOrdering.dto.analytics;

public record TopItemResponse(
        String itemName,
        long quantitySold
) {}
