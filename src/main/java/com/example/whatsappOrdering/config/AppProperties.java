package com.example.whatsappOrdering.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Frontend frontend = new Frontend();
    private Whatsapp whatsapp = new Whatsapp();
    private Cloudinary cloudinary = new Cloudinary();

    @Data
    public static class Jwt {
        private String secret = "change-this-in-production-must-be-256-bits";
        private long expirationMs = 86_400_000L; // 24 hours
    }

    @Data
    public static class Frontend {
        private String origin = "http://localhost:5173";
    }

    @Data
    public static class Whatsapp {
        private String token = "";
        private String phoneNumberId = "";
        private String verifyToken = "whatsapp_webhook_verify_token";
        private String apiUrl = "https://graph.facebook.com/v20.0";
        private int sessionTimeoutMinutes = 30;
    }

    @Data
    public static class Cloudinary {
        private String url = "";
    }
}
