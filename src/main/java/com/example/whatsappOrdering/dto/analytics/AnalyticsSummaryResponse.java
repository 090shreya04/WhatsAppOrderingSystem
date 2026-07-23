package com.example.whatsappOrdering.dto.analytics;

import java.math.BigDecimal;

public record AnalyticsSummaryResponse(
        long totalOrders,
        long dineInOrders,
        long whatsappOrders,
        BigDecimal totalRevenue
) {}
