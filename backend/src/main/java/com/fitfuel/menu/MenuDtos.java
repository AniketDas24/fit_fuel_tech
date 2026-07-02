package com.fitfuel.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

final class MenuDtos {
    private MenuDtos() {
    }
}

record FoodItemRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotNull MenuType menuType,
        @PositiveOrZero Integer protein,
        @PositiveOrZero Integer calories,
        @NotNull @PositiveOrZero BigDecimal price,
        Boolean active
) {
}

record FoodItemResponse(Long id, String name, String description, String category, MenuType menuType,
                        Integer protein, Integer calories, BigDecimal price, boolean active) {
    static FoodItemResponse from(FoodItem item) {
        return new FoodItemResponse(item.getId(), item.getName(), item.getDescription(), item.getCategory(),
                item.getMenuType(), item.getProtein(), item.getCalories(), item.getPrice(), item.isActive());
    }
}
