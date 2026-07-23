package com.example.whatsappOrdering.dto.menu;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        Long categoryId,
        String imageUrl,
        boolean available
) {}
