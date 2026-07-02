package com.fitfuel.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByMenuTypeAndActiveTrue(MenuType menuType);

    List<FoodItem> findByActiveTrue();
}
