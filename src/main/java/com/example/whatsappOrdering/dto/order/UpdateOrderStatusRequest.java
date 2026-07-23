package com.example.whatsappOrdering.dto.order;

import com.example.whatsappOrdering.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {}
