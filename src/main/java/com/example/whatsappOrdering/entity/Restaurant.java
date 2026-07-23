package com.example.whatsappOrdering.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    /** Random UUID slug embedded in public QR URLs — not the raw ID */
    @Column(name = "qr_secret", unique = true, nullable = false, length = 64)
    private String qrSecret;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (qrSecret == null || qrSecret.isBlank()) {
            qrSecret = UUID.randomUUID().toString().replace("-", "");
        }
    }
}
