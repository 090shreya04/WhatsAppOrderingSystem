package com.example.whatsappOrdering.dto.table;

import com.example.whatsappOrdering.entity.enums.TableStatus;

public record TableResponse(
        Long id,
        String tableNumber,
        TableStatus status,
        String qrCodeUrl
) {}
