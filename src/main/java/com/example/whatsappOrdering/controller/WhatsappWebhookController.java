package com.example.whatsappOrdering.controller;

import com.example.whatsappOrdering.config.AppProperties;
import com.example.whatsappOrdering.service.WhatsappService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp Webhook")
public class WhatsappWebhookController {

    private final AppProperties appProperties;
    private final WhatsappService whatsappService;
    private final ObjectMapper objectMapper;

    /**
     * One-time verification handshake when setting up the webhook in Meta console.
     * Meta sends GET with hub.mode=subscribe, hub.verify_token, hub.challenge.
     * We must echo hub.challenge if verify_token matches.
     */
    @GetMapping
    @Operation(summary = "Meta webhook verification (GET)")
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) &&
                appProperties.getWhatsapp().getVerifyToken().equals(verifyToken)) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed. Received token: {}", verifyToken);
        return ResponseEntity.status(403).body("Verification failed");
    }

    /**
     * Receives inbound WhatsApp messages from Meta Cloud API.
     * Must return 200 OK quickly — processing is synchronous but lightweight.
     */
    @PostMapping
    @Operation(summary = "Receive inbound WhatsApp messages (POST)")
    public ResponseEntity<String> receiveMessage(@RequestBody String rawBody) {
        log.debug("WhatsApp webhook raw payload: {}", rawBody);
        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> entries = (java.util.List<Map<String, Object>>) payload.get("entry");
            if (entries == null || entries.isEmpty()) return ResponseEntity.ok("ok");

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> changes = (java.util.List<Map<String, Object>>) entries.get(0).get("changes");
            if (changes == null || changes.isEmpty()) return ResponseEntity.ok("ok");

            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) changes.get(0).get("value");
            if (value == null) return ResponseEntity.ok("ok");

            // ── Handle delivery STATUS updates (sent/delivered/read/failed) ──────
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> statuses = (java.util.List<Map<String, Object>>) value.get("statuses");
            if (statuses != null && !statuses.isEmpty()) {
                for (Map<String, Object> status : statuses) {
                    String msgId     = (String) status.getOrDefault("id", "");
                    String statusVal = (String) status.getOrDefault("status", "");
                    String recipient = (String) status.getOrDefault("recipient_id", "");
                    Object errorObj  = status.get("errors");
                    if ("failed".equals(statusVal) || errorObj != null) {
                        log.error("⚠️ WhatsApp DELIVERY FAILED | msgId={} | to={} | errors={}", msgId, recipient, errorObj);
                    } else {
                        log.info("📬 WhatsApp delivery status: {} | msgId={} | to={}", statusVal, msgId, recipient);
                    }
                }
                return ResponseEntity.ok("ok");
            }

            // ── Handle inbound MESSAGES ───────────────────────────────────────────
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> messages = (java.util.List<Map<String, Object>>) value.get("messages");
            if (messages == null || messages.isEmpty()) {
                log.debug("WhatsApp webhook received with no messages or statuses — ignoring. value keys: {}", value.keySet());
                return ResponseEntity.ok("ok");
            }

            // Extract the business number that received this message
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) value.get("metadata");
            String displayPhoneNumber = metadata != null ? (String) metadata.getOrDefault("display_phone_number", "") : "";

            Map<String, Object> message = messages.get(0);
            String from = (String) message.getOrDefault("from", "");
            String messageId = (String) message.getOrDefault("id", "");
            String type = (String) message.getOrDefault("type", "");

            String body = "";
            if ("text".equals(type)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                body = textObj != null ? ((String) textObj.getOrDefault("body", "")).trim() : "";
            } else {
                log.info("Received non-text message of type '{}' from {}, treating as 'Menu'", type, from);
                body = "Menu";
            }

            if (body.isBlank()) body = "Menu";

            log.info("Inbound WhatsApp from={} to={}: {}", from, displayPhoneNumber, body);
            whatsappService.handleInbound(displayPhoneNumber, from, body, messageId);

        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook: {}", e.getMessage(), e);
            // Return 200 anyway — otherwise Meta will retry and flood us
        }

        return ResponseEntity.ok("ok");
    }
}
