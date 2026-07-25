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

    @Bean
    public org.springframework.boot.CommandLineRunner initData(
            com.example.whatsappOrdering.repository.UserRepository userRepository,
            com.example.whatsappOrdering.repository.RestaurantRepository restaurantRepository,
            com.example.whatsappOrdering.repository.CategoryRepository categoryRepository,
            com.example.whatsappOrdering.repository.MenuItemRepository menuItemRepository,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            if (restaurantRepository.count() == 0) {
                // 1. Create User / Owner
                com.example.whatsappOrdering.entity.User owner = userRepository.findByEmail("admin@tasty.com")
                        .orElseGet(() -> userRepository.save(com.example.whatsappOrdering.entity.User.builder()
                                .email("admin@tasty.com")
                                .passwordHash(passwordEncoder.encode("admin123"))
                                .name("Tasty Owner")
                                .role(com.example.whatsappOrdering.entity.enums.UserRole.OWNER)
                                .createdAt(java.time.LocalDateTime.now())
                                .build()));

                // 2. Create Restaurant
                com.example.whatsappOrdering.entity.Restaurant restaurant = restaurantRepository.save(
                        com.example.whatsappOrdering.entity.Restaurant.builder()
                                .name("Tasty Restaurant")
                                .whatsappNumber("15551640709")
                                .address("Main Market Road")
                                .qrSecret(java.util.UUID.randomUUID().toString().replace("-", ""))
                                .active(true)
                                .owner(owner)
                                .createdAt(java.time.LocalDateTime.now())
                                .build());

                // 3. Create Category
                com.example.whatsappOrdering.entity.Category starters = categoryRepository.save(
                        com.example.whatsappOrdering.entity.Category.builder()
                                .name("Starters")
                                .displayOrder(1)
                                .restaurant(restaurant)
                                .build());

                // 4. Create Menu Items
                menuItemRepository.save(com.example.whatsappOrdering.entity.MenuItem.builder()
                        .name("Egg Roll")
                        .price(new java.math.BigDecimal("70"))
                        .available(true)
                        .category(starters)
                        .restaurant(restaurant)
                        .build());

                menuItemRepository.save(com.example.whatsappOrdering.entity.MenuItem.builder()
                        .name("Chicken Roll")
                        .price(new java.math.BigDecimal("120"))
                        .available(true)
                        .category(starters)
                        .restaurant(restaurant)
                        .build());

                menuItemRepository.save(com.example.whatsappOrdering.entity.MenuItem.builder()
                        .name("Cold Drink")
                        .price(new java.math.BigDecimal("40"))
                        .available(true)
                        .category(starters)
                        .restaurant(restaurant)
                        .build());

                System.out.println("✅ Database Seeder: Successfully seeded Tasty Restaurant & Menu Items!");
            } else {
                // Ensure all existing restaurants have whatsappNumber = 15551640709 so WhatsApp bot maps correctly
                restaurantRepository.findAll().forEach(r -> {
                    if (r.getWhatsappNumber() == null || r.getWhatsappNumber().isBlank() || !r.getWhatsappNumber().equals("15551640709")) {
                        r.setWhatsappNumber("15551640709");
                        restaurantRepository.save(r);
                    }
                });
            }
        };
    }
}
