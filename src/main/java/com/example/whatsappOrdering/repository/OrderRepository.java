package com.example.whatsappOrdering.repository;

import com.example.whatsappOrdering.entity.Order;
import com.example.whatsappOrdering.entity.enums.OrderChannel;
import com.example.whatsappOrdering.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Live queue — all non-terminal orders (the main dashboard query) */
    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId " +
           "AND o.status NOT IN :excludedStatuses ORDER BY o.createdAt ASC")
    List<Order> findActiveOrders(@Param("restaurantId") Long restaurantId,
                                  @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    /** All orders with optional channel filter, newest first */
    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
    List<Order> findByRestaurantIdAndChannelOrderByCreatedAtDesc(Long restaurantId, OrderChannel channel);
    List<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(Long restaurantId, OrderStatus status);
    List<Order> findByRestaurantIdAndChannelAndStatusOrderByCreatedAtDesc(Long restaurantId,
                                                                           OrderChannel channel,
                                                                           OrderStatus status);

    Optional<Order> findByIdAndRestaurantId(Long id, Long restaurantId);

    /** Analytics date-range query */
    @Query("SELECT o FROM Order o WHERE o.restaurant.id = :restaurantId " +
           "AND o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED'")
    List<Order> findOrdersInDateRange(@Param("restaurantId") Long restaurantId,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /** Orders grouped by channel for analytics */
    @Query("SELECT o.channel, COUNT(o) FROM Order o WHERE o.restaurant.id = :restaurantId " +
           "AND o.createdAt BETWEEN :from AND :to AND o.status <> 'CANCELLED' GROUP BY o.channel")
    List<Object[]> countByChannel(@Param("restaurantId") Long restaurantId,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
