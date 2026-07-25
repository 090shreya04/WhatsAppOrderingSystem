package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByMenuItemId(Long menuItemId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM OrderItem oi WHERE oi.menuItem.id = :menuItemId")
    void deleteByMenuItemId(@Param("menuItemId") Long menuItemId);

    /** Top-selling items by quantity in a date range (analytics) */
    @Query("SELECT oi.menuItem.name, SUM(oi.quantity) AS total FROM OrderItem oi " +
           "WHERE oi.order.restaurant.id = :restaurantId " +
           "AND oi.order.createdAt BETWEEN :from AND :to " +
           "AND oi.order.status <> 'CANCELLED' " +
           "GROUP BY oi.menuItem.name ORDER BY total DESC")
    List<Object[]> findTopSellingItems(@Param("restaurantId") Long restaurantId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    /** Peak hours — PostgreSQL EXTRACT function via native query */
    @Query(value = "SELECT EXTRACT(HOUR FROM o.created_at)::INT AS hour, COUNT(DISTINCT o.id) AS cnt " +
                   "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
                   "WHERE o.restaurant_id = :restaurantId " +
                   "AND o.created_at BETWEEN :from AND :to " +
                   "AND o.status <> 'CANCELLED' " +
                   "GROUP BY EXTRACT(HOUR FROM o.created_at) ORDER BY hour",
           nativeQuery = true)
    List<Object[]> findPeakHours(@Param("restaurantId") Long restaurantId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
