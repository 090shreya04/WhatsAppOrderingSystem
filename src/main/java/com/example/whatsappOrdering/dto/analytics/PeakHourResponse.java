package com.example.whatsappOrdering.dto.analytics;

public record PeakHourResponse(
        int hour,
        long orderCount
) {}
