package com.fitfuel.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Carts are ephemeral working state (not historical records), so it's safe to
    // silently drop stray cart entries for a food item an admin is deleting.
    void deleteByFoodItem_Id(Long foodItemId);
}
