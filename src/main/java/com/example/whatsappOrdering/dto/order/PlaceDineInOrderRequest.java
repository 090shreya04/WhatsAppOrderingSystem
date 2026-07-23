package com.example.whatsappOrdering.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PlaceDineInOrderRequest(
        @NotBlank String qrSecret,
        Long tableId,
        @NotEmpty @Valid List<OrderItemRequest> items
) {}
