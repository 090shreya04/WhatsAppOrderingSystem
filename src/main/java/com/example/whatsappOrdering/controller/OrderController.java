package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.dto.order.*;
import com.example.whatsappOrdering.entity.enums.OrderStatus;
import com.example.whatsappOrdering.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    /** Public — called by customer after adding items to cart */
    @PostMapping("/public/orders")
    @Operation(summary = "Place a dine-in order (no auth)")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceDineInOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeDineInOrder(request));
    }

    /** Public — polling endpoint for customer order status */
    @GetMapping("/public/orders/{orderId}/status")
    @Operation(summary = "Poll order status (no auth — customer tracking)")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderStatus(orderId));
    }

    /** Owner — live queue with optional filters */
    @GetMapping("/restaurants/me/orders")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get order queue (owner), filterable by channel and status")
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            Authentication auth) {
        return ResponseEntity.ok(orderService.listOrders((Long) auth.getPrincipal(), channel, status));
    }

    /** Owner/staff — advance order through lifecycle */
    @PatchMapping("/orders/{id}/status")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update order status (staff)")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateOrderStatusRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus((Long) auth.getPrincipal(), id, request.status()));
    }
}
