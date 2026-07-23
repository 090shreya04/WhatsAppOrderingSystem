package com.example.whatsappOrdering.dto.table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank @Size(max = 10) String tableNumber
) {}
