package com.example.whatsappOrdering.service;

import com.example.whatsappOrdering.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * HTTP client wrapping Meta WhatsApp Cloud API v20.0.
 * Uses Spring's RestClient (Boot 3.2+/4.x).
 * @Retryable: one automatic retry with 1s backoff if the API call fails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsappApiClient {

    private final AppProperties appProperties;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void sendMessage(String to, String body) {
        AppProperties.Whatsapp cfg = appProperties.getWhatsapp();

        if (cfg.getToken() == null || cfg.getToken().isBlank() || cfg.getToken().equals("your_permanent_system_user_token")) {
            log.warn("WhatsApp token not configured (or is dummy). Skipping real API call to {}: {}", to, body);
            return;
        }

        String url = cfg.getApiUrl() + "/" + cfg.getPhoneNumberId() + "/messages";

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", body)
        );

        try {
            RestClient.create()
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + cfg.getToken())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp message sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", to, e.getMessage());
            throw e; // trigger retry
        }
    }
}
