package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByOwnerId(Long ownerId);
    Optional<Restaurant> findByQrSecret(String qrSecret);
    Optional<Restaurant> findByWhatsappNumber(String whatsappNumber);
    boolean existsByOwnerId(Long ownerId);
}
