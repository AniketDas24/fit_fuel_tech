package com.fitfuel.menu;

import com.fitfuel.common.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final FoodItemRepository foodItemRepository;

    public MenuController(FoodItemRepository foodItemRepository) {
        this.foodItemRepository = foodItemRepository;
    }

    @GetMapping
    List<FoodItemResponse> list(@RequestParam(required = false) MenuType type) {
        List<FoodItem> items = type == null
                ? foodItemRepository.findByActiveTrue()
                : foodItemRepository.findByMenuTypeAndActiveTrue(type);
        return items.stream().map(FoodItemResponse::from).toList();
    }

    @GetMapping("/{id}")
    FoodItemResponse get(@PathVariable Long id) {
        return FoodItemResponse.from(foodItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    FoodItemResponse create(@Valid @RequestBody FoodItemRequest request) {
        FoodItem item = new FoodItem();
        apply(item, request);
        return FoodItemResponse.from(foodItemRepository.save(item));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    FoodItemResponse update(@PathVariable Long id, @Valid @RequestBody FoodItemRequest request) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found"));
        apply(item, request);
        return FoodItemResponse.from(foodItemRepository.save(item));
    }

    private void apply(FoodItem item, FoodItemRequest request) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setCategory(request.category());
        item.setMenuType(request.menuType());
        item.setProtein(request.protein());
        item.setCalories(request.calories());
        item.setPrice(request.price());
        item.setActive(request.active() == null || request.active());
    }
}
