package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.analytics.AnalyticsSummaryResponse;
import com.example.whatsappOrdering.dto.analytics.PeakHourResponse;
import com.example.whatsappOrdering.dto.analytics.TopItemResponse;
import com.example.whatsappOrdering.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants/me/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Orders by channel + revenue in date range")
    public ResponseEntity<AnalyticsSummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getSummary((Long) auth.getPrincipal(), from, to));
    }

    @GetMapping("/top-items")
    @Operation(summary = "Top-selling items by quantity")
    public ResponseEntity<List<TopItemResponse>> topItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getTopItems((Long) auth.getPrincipal(), from, to));
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Order count by hour of day")
    public ResponseEntity<List<PeakHourResponse>> peakHours(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication auth) {
        return ResponseEntity.ok(analyticsService.getPeakHours((Long) auth.getPrincipal(), from, to));
    }
}
