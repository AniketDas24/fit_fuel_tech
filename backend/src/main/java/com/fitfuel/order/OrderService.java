package com.fitfuel.order;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.menu.FoodItem;
import com.fitfuel.menu.FoodItemRepository;
import com.fitfuel.menu.MenuType;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public OrderService(UserRepository userRepository, FoodItemRepository foodItemRepository,
                        CartRepository cartRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public CartResponse addToCart(String email, AddCartItemRequest request) {
        AppUser user = user(email);
        FoodItem foodItem = foodItemRepository.findById(request.foodItemId())
                .orElseThrow(() -> new NotFoundException("Food item not found"));
        if (!foodItem.isActive() || foodItem.getMenuType() != MenuType.REGULAR_MENU) {
            throw new IllegalArgumentException("Only active regular menu items can be ordered");
        }
        Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
            Cart created = new Cart();
            created.setUser(user);
            return created;
        });
        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getFoodItem().getId().equals(foodItem.getId()))
                .findFirst()
                .orElseGet(() -> {
                    CartItem created = new CartItem();
                    created.setCart(cart);
                    created.setFoodItem(foodItem);
                    cart.getItems().add(created);
                    return created;
                });
        item.setQuantity(item.getQuantity() + request.quantity());
        return toCartResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse currentCart(String email) {
        return toCartResponse(cartRepository.findByUser(user(email)).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user(email));
            return cartRepository.save(cart);
        }));
    }

    @Transactional
    public OrderResponse checkout(String email) {
        AppUser user = user(email);
        Cart cart = cartRepository.findByUser(user).orElseThrow(() -> new IllegalArgumentException("Cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setFoodItem(cartItem.getFoodItem());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getFoodItem().getPrice());
            order.getItems().add(orderItem);
        }
        order.setTotalAmount(total(order));
        cart.getItems().clear();
        cartRepository.save(cart);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public java.util.List<OrderResponse> orders(String email) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user(email))
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private CartResponse toCartResponse(Cart cart) {
        var items = cart.getItems().stream()
                .map(item -> new CartItemResponse(item.getFoodItem().getId(), item.getFoodItem().getName(),
                        item.getQuantity(), item.getFoodItem().getPrice()))
                .toList();
        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getFoodItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }

    private BigDecimal total(CustomerOrder order) {
        return order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
