package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdOrderByCategoryIdAscNameAsc(Long restaurantId);
    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByCategoryIdAscNameAsc(Long restaurantId);
    List<MenuItem> findByCategoryId(Long categoryId);
    Optional<MenuItem> findByIdAndRestaurantId(Long id, Long restaurantId);
    void deleteByIdAndRestaurantId(Long id, Long restaurantId);
}
