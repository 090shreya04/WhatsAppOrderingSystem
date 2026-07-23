package com.example.whatsappOrdering.entity;

import com.example.whatsappOrdering.entity.enums.WhatsappSessionState;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_sessions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "customer_phone"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsappSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private WhatsappSessionState state = WhatsappSessionState.AWAITING_ITEMS;

    /**
     * JSON blob containing the menu numbering map (state=AWAITING_ITEMS)
     * or the parsed cart items (state=CONFIRMING).
     */
    @Column(name = "pending_order_json", columnDefinition = "TEXT")
    private String pendingOrderJson;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @PrePersist
    @PreUpdate
    protected void onActivity() {
        if (lastMessageAt == null) {
            lastMessageAt = LocalDateTime.now();
        }
    }
}
