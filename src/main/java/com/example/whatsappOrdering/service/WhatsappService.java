package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.config.AppProperties;
import com.example.whatsappOrdering.entity.*;
import com.example.whatsappOrdering.entity.enums.*;
import com.example.whatsappOrdering.repository.*;
import com.example.whatsappOrdering.websocket.OrderWebSocketPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * WhatsApp conversation state machine.
 *
 * States:
 *   AWAITING_ITEMS → send numbered menu list, wait for selection
 *   CONFIRMING     → parse selection, show total, ask YES/NO
 *   DONE           → order created; resets to AWAITING_ITEMS on next message
 *
 * Input format: "1, 3x2, 2"  →  item 1 qty 1, item 3 qty 2, item 2 qty 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsappService {

    private final WhatsappSessionRepository sessionRepository;
    private final WhatsappMessageRepository messageRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WhatsappApiClient apiClient;
    private final OrderWebSocketPublisher webSocketPublisher;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    // ─── Entry point from webhook ─────────────────────────────────────

    @Transactional
    public void handleInbound(String displayPhoneNumber, String customerPhone,
                               String messageBody, String whatsappMessageId) {
        // Normalize phone: strip leading + if present
        String normalizedDisplay = displayPhoneNumber.replaceAll("[^\\d]", "");
        String normalizedCustomer = customerPhone.replaceAll("[^\\d]", "");

        Restaurant restaurant = restaurantRepository.findByWhatsappNumber(normalizedDisplay)
                .orElse(null);
        if (restaurant == null) {
            log.warn("No restaurant found for WhatsApp number: {}", displayPhoneNumber);
            return;
        }

        // Log inbound message
        logMessage(restaurant, normalizedCustomer, MessageDirection.IN, messageBody,
                whatsappMessageId, null);

        // Get or create session
        WhatsappSession session = sessionRepository
                .findByRestaurantIdAndCustomerPhone(restaurant.getId(), normalizedCustomer)
                .orElse(null);

        // Session expiry check
        if (session != null && isExpired(session)) {
            log.info("Session expired for {}, resetting", normalizedCustomer);
            session.setState(WhatsappSessionState.AWAITING_ITEMS);
            session.setPendingOrderJson(null);
        }

        if (session == null) {
            session = WhatsappSession.builder()
                    .restaurant(restaurant)
                    .customerPhone(normalizedCustomer)
                    .state(WhatsappSessionState.AWAITING_ITEMS)
                    .lastMessageAt(LocalDateTime.now())
                    .build();
            session = sessionRepository.save(session);
        }

        // Update last activity
        session.setLastMessageAt(LocalDateTime.now());

        // Route to state handler
        switch (session.getState()) {
            case AWAITING_ITEMS -> handleAwaitingItemsRouted(session, restaurant, normalizedCustomer, messageBody);
            case CONFIRMING     -> handleConfirming(session, restaurant, normalizedCustomer, messageBody);
            case DONE           -> {
                // Any new message after DONE restarts the flow
                session.setState(WhatsappSessionState.AWAITING_ITEMS);
                session.setPendingOrderJson(null);
                handleAwaitingItemsRouted(session, restaurant, normalizedCustomer, messageBody);
            }
        }
        sessionRepository.save(session);
    }

    // ─── State handlers ───────────────────────────────────────────────

    private void handleAwaitingItems(WhatsappSession session, Restaurant restaurant,
                                      String customerPhone, String messageBody) {
        List<MenuItem> items = menuItemRepository
                .findByRestaurantIdAndAvailableTrueOrderByCategoryIdAscNameAsc(restaurant.getId());

        if (items.isEmpty()) {
            sendAndLog(restaurant, customerPhone, "Sorry, our menu is empty right now. Please try again later.", null);
            return;
        }

        // Build numbered menu map: {num -> menuItemId}
        Map<Integer, Long> menuMap = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            menuMap.put(i + 1, items.get(i).getId());
        }

        // Build menu text
        StringBuilder sb = new StringBuilder();
        sb.append("Welcome to *").append(restaurant.getName()).append("*! 🍽️\n\n");

        // Group by category
        String lastCatName = null;
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            String catName = item.getCategory() != null ? item.getCategory().getName() : "Menu";
            if (!catName.equals(lastCatName)) {
                sb.append("\n*").append(catName).append("*\n");
                lastCatName = catName;
            }
            sb.append(String.format("%d. %s - ₹%.0f%n", i + 1, item.getName(), item.getPrice()));
        }

        sb.append("\n📝 *Reply with item numbers & qty:*");
        sb.append("\nExample: `1, 3x2, 2` (item 1 qty 1, item 3 qty 2, item 2 qty 1)");

        // Store menu numbering for later parsing
        try {
            session.setPendingOrderJson(objectMapper.writeValueAsString(menuMap));
        } catch (Exception e) {
            log.error("Failed to serialize menu map", e);
        }
        session.setState(WhatsappSessionState.AWAITING_ITEMS);

        sendAndLog(restaurant, customerPhone, sb.toString(), null);
    }

    private void handleConfirming(WhatsappSession session, Restaurant restaurant,
                                   String customerPhone, String messageBody) {
        String reply = messageBody.trim().toUpperCase();

        if (reply.equals("YES") || reply.equals("Y")) {
            createWhatsappOrder(session, restaurant, customerPhone);
        } else if (reply.equals("NO") || reply.equals("N")) {
            session.setState(WhatsappSessionState.AWAITING_ITEMS);
            session.setPendingOrderJson(null);
            sendAndLog(restaurant, customerPhone,
                    "Order cancelled. Reply with item numbers to start a new order.", null);
        } else {
            // Re-ask
            sendAndLog(restaurant, customerPhone,
                    "Please reply *YES* to confirm or *NO* to cancel.", null);
        }
    }

    // ─── Parse selection & transition to CONFIRMING ───────────────────

    /**
     * Parses "2, 5x2, 8" and transitions the session to CONFIRMING state.
     * Called when we receive a message in AWAITING_ITEMS state that looks like item selections.
     */
    private void parseSelectionAndConfirm(WhatsappSession session, Restaurant restaurant,
                                           String customerPhone, String messageBody) {
        Map<Integer, Long> menuMap;
        try {
            menuMap = objectMapper.readValue(session.getPendingOrderJson(),
                    new TypeReference<LinkedHashMap<Integer, Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse menu map from session", e);
            sendAndLog(restaurant, customerPhone,
                    "Sorry, something went wrong. Please send any message to restart.", null);
            session.setState(WhatsappSessionState.AWAITING_ITEMS);
            session.setPendingOrderJson(null);
            return;
        }

        // Parse the selection
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)(?:x(\\d+))?");
        java.util.regex.Matcher m = p.matcher(messageBody);
        List<Map<String, Object>> cart = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        while (m.find()) {
            int num = Integer.parseInt(m.group(1));
            int qty = m.group(2) != null ? Integer.parseInt(m.group(2)) : 1;

            Long menuItemId = menuMap.get(num);
            if (menuItemId == null) {
                errors.add("Item #" + num + " not found");
                continue;
            }

            menuItemRepository.findById(menuItemId).ifPresent(item -> {
                Map<String, Object> cartItem = new LinkedHashMap<>();
                cartItem.put("menuItemId", item.getId());
                cartItem.put("name", item.getName());
                cartItem.put("quantity", qty);
                cartItem.put("price", item.getPrice().doubleValue());
                cart.add(cartItem);
            });
        }

        if (cart.isEmpty()) {
            sendAndLog(restaurant, customerPhone,
                    "I couldn't understand your order. Please reply with item numbers like:\n`1, 3x2, 2`", null);
            return;
        }

        if (!errors.isEmpty()) {
            sendAndLog(restaurant, customerPhone,
                    "Some items were invalid: " + String.join(", ", errors) +
                    "\nPlease try again.", null);
            return;
        }

        // Compute total & build confirmation message
        double total = cart.stream()
                .mapToDouble(i -> ((Number) i.get("price")).doubleValue() * ((Number) i.get("quantity")).intValue())
                .sum();

        StringBuilder sb = new StringBuilder("📋 *Your Order:*\n");
        for (Map<String, Object> item : cart) {
            sb.append(String.format("• %s x%d = ₹%.0f%n",
                    item.get("name"),
                    ((Number) item.get("quantity")).intValue(),
                    ((Number) item.get("price")).doubleValue() * ((Number) item.get("quantity")).intValue()));
        }
        sb.append(String.format("\n💰 *Total: ₹%.0f*\n", total));
        sb.append("\nReply *YES* to confirm or *NO* to cancel.");

        try {
            session.setPendingOrderJson(objectMapper.writeValueAsString(cart));
        } catch (Exception e) {
            log.error("Failed to serialize cart", e);
        }
        session.setState(WhatsappSessionState.CONFIRMING);
        sendAndLog(restaurant, customerPhone, sb.toString(), null);
    }

    // ─── Override handleAwaitingItems to detect item selection ────────

    // (Re-route logic: if message body contains numbers it's a selection, else send menu)
    @Transactional
    protected void handleAwaitingItemsRouted(WhatsappSession session, Restaurant restaurant,
                                              String customerPhone, String messageBody) {
        boolean hasNumbers = messageBody.matches(".*\\d.*");
        if (hasNumbers && session.getPendingOrderJson() != null) {
            parseSelectionAndConfirm(session, restaurant, customerPhone, messageBody);
        } else {
            handleAwaitingItems(session, restaurant, customerPhone, messageBody);
        }
    }

    // ─── Create WhatsApp order ────────────────────────────────────────

    private void createWhatsappOrder(WhatsappSession session, Restaurant restaurant,
                                      String customerPhone) {
        List<Map<String, Object>> cart;
        try {
            cart = objectMapper.readValue(session.getPendingOrderJson(),
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse cart for order creation", e);
            sendAndLog(restaurant, customerPhone,
                    "Something went wrong. Please start over by sending any message.", null);
            session.setState(WhatsappSessionState.AWAITING_ITEMS);
            session.setPendingOrderJson(null);
            return;
        }

        Order order = Order.builder()
                .restaurant(restaurant)
                .customerPhone(customerPhone)
                .channel(OrderChannel.WHATSAPP)
                .status(OrderStatus.PLACED)
                .build();
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Map<String, Object> ci : cart) {
            Long menuItemId = Long.valueOf(ci.get("menuItemId").toString());
            int qty = ((Number) ci.get("quantity")).intValue();
            MenuItem menuItem = menuItemRepository.findById(menuItemId).orElse(null);
            if (menuItem == null) continue;

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(qty)
                    .priceAtOrder(menuItem.getPrice())
                    .build();
            items.add(orderItemRepository.save(oi));
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        session.setState(WhatsappSessionState.DONE);
        session.setPendingOrderJson(null);

        // Push to owner dashboard
        webSocketPublisher.publishOrderCreated(order);

        // Confirmation message to customer
        String confirmMsg = String.format(
                "✅ Order #%d confirmed! Total: ₹%.0f\n\nWe'll send you updates when your order is ready. 🙏",
                order.getId(), total.doubleValue());
        sendAndLog(restaurant, customerPhone, confirmMsg, order);

        log.info("WhatsApp order {} created for {}", order.getId(), customerPhone);
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private void sendAndLog(Restaurant restaurant, String customerPhone,
                             String messageBody, Order order) {
        apiClient.sendMessage(customerPhone, messageBody);
        logMessage(restaurant, customerPhone, MessageDirection.OUT, messageBody, null, order);
    }

    private void logMessage(Restaurant restaurant, String customerPhone,
                             MessageDirection direction, String body,
                             String whatsappMessageId, Order order) {
        WhatsappMessage msg = WhatsappMessage.builder()
                .restaurant(restaurant)
                .customerPhone(customerPhone)
                .direction(direction)
                .messageBody(body)
                .whatsappMessageId(whatsappMessageId)
                .status(direction == MessageDirection.OUT ? "sent" : null)
                .order(order)
                .build();
        messageRepository.save(msg);
    }

    private boolean isExpired(WhatsappSession session) {
        int timeoutMinutes = appProperties.getWhatsapp().getSessionTimeoutMinutes();
        return session.getLastMessageAt().isBefore(
                LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    /** Scheduled cleanup of stale sessions every 15 minutes */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(appProperties.getWhatsapp().getSessionTimeoutMinutes());
        List<WhatsappSession> expired = sessionRepository.findExpiredSessions(cutoff);
        for (WhatsappSession s : expired) {
            if (s.getState() != WhatsappSessionState.DONE) {
                s.setState(WhatsappSessionState.AWAITING_ITEMS);
                s.setPendingOrderJson(null);
                sessionRepository.save(s);
            }
        }
        if (!expired.isEmpty()) {
            log.debug("Reset {} expired WhatsApp sessions", expired.size());
        }
    }
}
