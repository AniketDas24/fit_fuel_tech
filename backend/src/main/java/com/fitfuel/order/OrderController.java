package com.fitfuel.order;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/cart")
    CartResponse cart(Authentication authentication) {
        return orderService.currentCart(authentication.getName());
    }

    @PostMapping("/cart/items")
    CartResponse addToCart(Authentication authentication, @Valid @RequestBody AddCartItemRequest request) {
        return orderService.addToCart(authentication.getName(), request);
    }

    @PostMapping("/orders/checkout")
    OrderResponse checkout(Authentication authentication, @RequestBody(required = false) CheckoutRequest request) {
        return orderService.checkout(authentication.getName());
    }

    @GetMapping("/orders")
    List<OrderResponse> orders(Authentication authentication) {
        return orderService.orders(authentication.getName());
    }
}
