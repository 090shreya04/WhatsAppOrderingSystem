package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    List<RestaurantTable> findByRestaurantIdOrderByTableNumberAsc(Long restaurantId);
    Optional<RestaurantTable> findByIdAndRestaurantId(Long id, Long restaurantId);
    boolean existsByRestaurantIdAndTableNumber(Long restaurantId, String tableNumber);
}
