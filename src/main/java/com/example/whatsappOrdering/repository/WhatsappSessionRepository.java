package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.WhatsappSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsappSessionRepository extends JpaRepository<WhatsappSession, Long> {

    /** Called on every inbound webhook message to find or create a session */
    List<WhatsappSession> findByRestaurantIdAndCustomerPhoneOrderByLastMessageAtDesc(Long restaurantId, String customerPhone);

    /** Cleanup: find stale sessions for scheduled expiry */
    @Query("SELECT s FROM WhatsappSession s WHERE s.lastMessageAt < :cutoff")
    List<WhatsappSession> findExpiredSessions(@Param("cutoff") LocalDateTime cutoff);
}
