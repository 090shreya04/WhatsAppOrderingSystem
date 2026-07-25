package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.dto.order.*;
import com.example.whatsappOrdering.entity.*;
import com.example.whatsappOrdering.entity.enums.OrderChannel;
import com.example.whatsappOrdering.entity.enums.OrderStatus;
import com.example.whatsappOrdering.exception.BusinessException;
import com.example.whatsappOrdering.exception.ResourceNotFoundException;
import com.example.whatsappOrdering.repository.*;
import com.example.whatsappOrdering.websocket.OrderWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderWebSocketPublisher webSocketPublisher;
    private final WhatsappApiClient whatsappApiClient;

    private static final List<OrderStatus> TERMINAL_STATUSES =
            List.of(OrderStatus.SERVED, OrderStatus.CANCELLED);

    // ─── Dine-In Order (public, no auth) ─────────────────────────────

    @Transactional
    public OrderResponse placeDineInOrder(PlaceDineInOrderRequest request) {
        Restaurant restaurant = restaurantRepository.findByQrSecret(request.qrSecret())
                .orElseThrow(() -> new BusinessException("Invalid QR code"));

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(request.tableId(), restaurant.getId())
                .orElseThrow(() -> new BusinessException("Table not found in this restaurant"));

        Order order = Order.builder()
                .restaurant(restaurant)
                .restaurantTable(table)
                .channel(OrderChannel.DINE_IN)
                .status(OrderStatus.PLACED)
                .build();

        List<OrderItem> items = buildOrderItems(order, request.items(), restaurant.getId());
        BigDecimal total = items.stream()
                .map(i -> i.getPriceAtOrder().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setItems(items);
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // Mark table as occupied
        table.setStatus(com.example.whatsappOrdering.entity.enums.TableStatus.OCCUPIED);
        tableRepository.save(table);

        // Push to dashboard within ~100ms via WebSocket
        webSocketPublisher.publishOrderCreated(order);
        log.info("Dine-in order {} created for table {}", order.getId(), table.getTableNumber());
        return toResponse(order);
    }

    // ─── Status Query (public polling) ───────────────────────────────

    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return new OrderStatusResponse(order.getId(), order.getStatus(), order.getUpdatedAt());
    }

    // ─── Owner Order Queue ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(Long ownerId, String channel, String status) {
        Restaurant restaurant = getRestaurantForOwner(ownerId);
        List<Order> orders;

        if (channel == null && status == null) {
            orders = orderRepository.findActiveOrders(restaurant.getId(), TERMINAL_STATUSES);
        } else if (channel != null && status != null) {
            orders = orderRepository.findByRestaurantIdAndChannelAndStatusOrderByCreatedAtDesc(
                    restaurant.getId(),
                    OrderChannel.valueOf(channel.toUpperCase()),
                    OrderStatus.valueOf(status.toUpperCase()));
        } else if (channel != null) {
            orders = orderRepository.findByRestaurantIdAndChannelOrderByCreatedAtDesc(
                    restaurant.getId(), OrderChannel.valueOf(channel.toUpperCase()));
        } else {
            orders = orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                    restaurant.getId(), OrderStatus.valueOf(status.toUpperCase()));
        }
        return orders.stream().map(this::toResponse).toList();
    }

    // ─── Update Status (owner/staff) ─────────────────────────────────

    @Transactional
    public OrderResponse updateOrderStatus(Long ownerId, Long orderId, OrderStatus newStatus, String reason) {
        Restaurant restaurant = getRestaurantForOwner(ownerId);
        Order order = orderRepository.findByIdAndRestaurantId(orderId, restaurant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order = orderRepository.save(order);

        // Push dashboard update
        webSocketPublisher.publishStatusChanged(order);

        // Notify WhatsApp customer
        if (order.getChannel() == OrderChannel.WHATSAPP && order.getCustomerPhone() != null) {
            sendWhatsAppStatusNotification(order, newStatus, reason);
        }

        log.info("Order {} status changed: {} → {}", orderId, oldStatus, newStatus);
        return toResponse(order);
    }

    // ─── Internal helpers ─────────────────────────────────────────────

    private List<OrderItem> buildOrderItems(Order order, List<OrderItemRequest> requests, Long restaurantId) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest req : requests) {
            MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(req.menuItemId(), restaurantId)
                    .orElseThrow(() -> new BusinessException(
                            "Menu item not available: " + req.menuItemId()));
            if (!menuItem.isAvailable()) {
                throw new BusinessException("Item is currently unavailable: " + menuItem.getName());
            }
            items.add(OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(req.quantity())
                    .priceAtOrder(menuItem.getPrice())   // server-side price snapshot
                    .build());
        }
        return items;
    }

    private Restaurant getRestaurantForOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new BusinessException("No restaurant found for this owner"));
    }

    private void sendWhatsAppStatusNotification(Order order, OrderStatus status, String reason) {
        try {
            String message;
            if (status == OrderStatus.CANCELLED) {
                if (reason != null && !reason.isBlank()) {
                    message = "❌ Your order #" + order.getId() + " has been cancelled. Reason: " + reason.trim();
                } else {
                    message = "❌ Sorry, your order #" + order.getId() + " has been cancelled. Please contact us for help.";
                }
            } else {
                message = switch (status) {
                    case CONFIRMED -> "✅ Your order has been confirmed! We're getting started on it.";
                    case PREPARING -> "🍳 Your order is being prepared!";
                    case READY -> "🎉 Your order is ready for pickup!";
                    case SERVED -> "🙏 Thank you for ordering from " + (order.getRestaurant() != null ? order.getRestaurant().getName() : "us") + "! Hope you enjoyed your meal. Visit us again soon! ❤️";
                    default -> null;
                };
            }
            if (message != null) {
                whatsappApiClient.sendMessage(order.getCustomerPhone(), message);
            }
        } catch (Exception e) {
            log.warn("Failed to send WhatsApp status notification for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }

    OrderResponse toResponse(Order order) {
        String tableNumber = order.getRestaurantTable() != null
                ? order.getRestaurantTable().getTableNumber() : null;
        Long tableId = order.getRestaurantTable() != null
                ? order.getRestaurantTable().getId() : null;
        List<OrderResponse.OrderItemDetail> itemDetails = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemDetail(
                        i.getMenuItem().getId(), i.getMenuItem().getName(),
                        i.getQuantity(), i.getPriceAtOrder()))
                .toList();
        return new OrderResponse(order.getId(), order.getChannel(), order.getStatus(),
                tableId, tableNumber, order.getCustomerPhone(), order.getTotalAmount(),
                itemDetails, order.getCreatedAt(), order.getUpdatedAt());
    }
}
