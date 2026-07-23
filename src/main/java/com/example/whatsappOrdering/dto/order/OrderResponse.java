package com.example.whatsappOrdering.dto.order;

import com.example.whatsappOrdering.entity.enums.OrderChannel;
import com.example.whatsappOrdering.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderChannel channel,
        OrderStatus status,
        Long tableId,
        String tableNumber,
        String customerPhone,
        BigDecimal totalAmount,
        List<OrderItemDetail> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record OrderItemDetail(
            Long menuItemId,
            String menuItemName,
            int quantity,
            BigDecimal priceAtOrder
    ) {}
}
