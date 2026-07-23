package com.example.whatsappOrdering.dto.order;

import com.example.whatsappOrdering.entity.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusResponse(
        Long orderId,
        OrderStatus status,
        LocalDateTime updatedAt
) {}
