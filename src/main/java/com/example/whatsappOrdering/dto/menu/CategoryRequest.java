package com.example.whatsappOrdering.dto.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @PositiveOrZero Integer displayOrder
) {}
