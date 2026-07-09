package com.fitfuel.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Called when an admin deletes a menu item: past order rows keep their snapshotted
    // foodItemName/price and simply lose the live reference, so order history stays intact.
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.foodItem = null WHERE oi.foodItem.id = :foodItemId")
    void detachFoodItem(@Param("foodItemId") Long foodItemId);
}
