package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.dto.analytics.AnalyticsSummaryResponse;
import com.example.whatsappOrdering.dto.analytics.PeakHourResponse;
import com.example.whatsappOrdering.dto.analytics.TopItemResponse;
import com.example.whatsappOrdering.entity.Order;
import com.example.whatsappOrdering.entity.enums.OrderChannel;
import com.example.whatsappOrdering.repository.OrderItemRepository;
import com.example.whatsappOrdering.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantService restaurantService;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long ownerId, LocalDate from, LocalDate to) {
        Long restaurantId = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId).getId();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository.findOrdersInDateRange(restaurantId, fromDt, toDt);
        long dineIn = orders.stream().filter(o -> o.getChannel() == OrderChannel.DINE_IN).count();
        long whatsapp = orders.stream().filter(o -> o.getChannel() == OrderChannel.WHATSAPP).count();
        BigDecimal revenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AnalyticsSummaryResponse(orders.size(), dineIn, whatsapp, revenue);
    }

    @Transactional(readOnly = true)
    public List<TopItemResponse> getTopItems(Long ownerId, LocalDate from, LocalDate to) {
        Long restaurantId = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId).getId();
        List<Object[]> rows = orderItemRepository.findTopSellingItems(
                restaurantId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return rows.stream()
                .map(r -> new TopItemResponse((String) r[0], ((Number) r[1]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PeakHourResponse> getPeakHours(Long ownerId, LocalDate from, LocalDate to) {
        Long restaurantId = restaurantService.getRestaurantByOwnerIdOrThrow(ownerId).getId();
        List<Object[]> rows = orderItemRepository.findPeakHours(
                restaurantId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return rows.stream()
                .map(r -> new PeakHourResponse(((Number) r[0]).intValue(), ((Number) r[1]).longValue()))
                .toList();
    }
}
