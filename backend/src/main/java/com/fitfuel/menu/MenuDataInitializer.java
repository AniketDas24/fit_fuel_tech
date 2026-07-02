package com.fitfuel.menu;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MenuDataInitializer implements CommandLineRunner {

    private final FoodItemRepository foodItemRepository;

    public MenuDataInitializer(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    public void run(String... args) {
        if (foodItemRepository.count() > 0) {
            return;
        }

        List<SeedFoodItem> items = List.of(
                new SeedFoodItem("Paneer Protein Bowl", "Paneer, rice, vegetables, curd, and salad", "BOWL", MenuType.REGULAR_MENU, 42, 620, "199"),
                new SeedFoodItem("Chicken Lean Box", "Grilled chicken, brown rice, sauteed vegetables, and salad", "BOX", MenuType.REGULAR_MENU, 48, 580, "229"),
                new SeedFoodItem("Egg Power Sandwich", "Egg sandwich with greens and house seasoning", "BREAKFAST", MenuType.REGULAR_MENU, 28, 410, "149"),
                new SeedFoodItem("Quinoa Veg Plate", "Quinoa, paneer tikka, sprouts, and mixed vegetables", "PLATE", MenuType.REGULAR_MENU, 36, 540, "189"),
                new SeedFoodItem("Lean & Clean Mess Menu", "Daily fat-loss mess menu managed by FitFuel", "MESS", MenuType.MESS_MENU, 100, 1500, "0"),
                new SeedFoodItem("Gainz Mess Menu", "Daily mass-gain mess menu managed by FitFuel", "MESS", MenuType.MESS_MENU, 130, 2400, "0")
        );

        foodItemRepository.saveAll(items.stream().map(this::toEntity).toList());
    }

    private FoodItem toEntity(SeedFoodItem seed) {
        FoodItem item = new FoodItem();
        item.setName(seed.name());
        item.setDescription(seed.description());
        item.setCategory(seed.category());
        item.setMenuType(seed.menuType());
        item.setProtein(seed.protein());
        item.setCalories(seed.calories());
        item.setPrice(new BigDecimal(seed.price()));
        item.setActive(true);
        return item;
    }

    private record SeedFoodItem(String name, String description, String category, MenuType menuType,
                                Integer protein, Integer calories, String price) {
    }
}
