package com.example.whatsappOrdering.websocket;

import com.example.whatsappOrdering.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC = "/topic/restaurant/{restaurantId}/orders";

    public void publishOrderCreated(Order order) {
        OrderEvent event = toEvent(order, "ORDER_CREATED");
        String destination = TOPIC.replace("{restaurantId}", String.valueOf(order.getRestaurant().getId()));
        log.debug("Publishing ORDER_CREATED to {}", destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    public void publishStatusChanged(Order order) {
        OrderEvent event = toEvent(order, "ORDER_STATUS_CHANGED");
        String destination = TOPIC.replace("{restaurantId}", String.valueOf(order.getRestaurant().getId()));
        // Also notify the specific order topic (for customer status polling alternative)
        String orderTopic = "/topic/order/" + order.getId();
        log.debug("Publishing ORDER_STATUS_CHANGED to {} and {}", destination, orderTopic);
        messagingTemplate.convertAndSend(destination, event);
        messagingTemplate.convertAndSend(orderTopic, event);
    }

    private OrderEvent toEvent(Order order, String type) {
        String tableNumber = order.getRestaurantTable() != null
                ? order.getRestaurantTable().getTableNumber() : null;
        return OrderEvent.builder()
                .type(type)
                .orderId(order.getId())
                .channel(order.getChannel())
                .status(order.getStatus())
                .tableNumber(tableNumber)
                .customerPhone(order.getCustomerPhone())
                .totalAmount(order.getTotalAmount())
                .build();
    }
}
