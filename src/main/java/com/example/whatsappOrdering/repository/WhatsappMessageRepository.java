package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.WhatsappMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {
    List<WhatsappMessage> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    List<WhatsappMessage> findByCustomerPhoneAndRestaurantIdOrderByCreatedAtDesc(
            String customerPhone, Long restaurantId);
}
