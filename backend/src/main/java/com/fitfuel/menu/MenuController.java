package com.fitfuel.menu;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.order.CartItemRepository;
import com.fitfuel.order.OrderItemRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final FoodItemRepository foodItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    public MenuController(FoodItemRepository foodItemRepository, OrderItemRepository orderItemRepository,
                          CartItemRepository cartItemRepository) {
        this.foodItemRepository = foodItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    List<FoodItemResponse> allIncludingInactive() {
        return foodItemRepository.findAllByOrderByCategoryAscNameAsc().stream().map(FoodItemResponse::from).toList();
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    void delete(@PathVariable Long id) {
        FoodItem item = foodItemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Food item not found"));
        if (orderItemRepository.existsByFoodItem_Id(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete \"" + item.getName() + "\" — it appears in existing orders. Deactivate it instead.");
        }
        cartItemRepository.deleteByFoodItem_Id(id); // safe to drop: carts are ephemeral, not historical records
        foodItemRepository.delete(item);
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
