package com.example.whatsappOrdering.websocket;

import com.example.whatsappOrdering.entity.enums.OrderChannel;
import com.example.whatsappOrdering.entity.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Payload pushed to /topic/restaurant/{restaurantId}/orders on:
 *   • ORDER_CREATED   — new order from either channel
 *   • ORDER_STATUS_CHANGED — staff moved order through lifecycle
 */
@Builder
public record OrderEvent(
        String type,          // ORDER_CREATED | ORDER_STATUS_CHANGED
        Long orderId,
        OrderChannel channel,
        OrderStatus status,
        String tableNumber,
        String customerPhone,
        BigDecimal totalAmount
) {}
