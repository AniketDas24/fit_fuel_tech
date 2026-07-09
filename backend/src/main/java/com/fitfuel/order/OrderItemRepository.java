package com.fitfuel.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByFoodItem_Id(Long foodItemId);
}
