package com.example.whatsappOrdering;

import com.example.whatsappOrdering.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
@EnableRetry          // enables @Retryable on WhatsappApiClient
@EnableScheduling     // enables @Scheduled for session cleanup
public class WhatsappOrderingApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsappOrderingApplication.class, args);
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
