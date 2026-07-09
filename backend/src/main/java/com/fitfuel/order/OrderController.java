package com.fitfuel.order;

import com.fitfuel.notification.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderController(OrderService orderService, OrderRepository orderRepository,
                           NotificationService notificationService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
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

    @GetMapping("/orders/all")
    @PreAuthorize("hasRole('ADMIN')")
    List<AdminOrderResponse> allOrders() {
        return orderService.adminOrders();
    }

    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    AdminOrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.transitionStatus(id, request.status());
    }

    @GetMapping(path = "/orders/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter notifications(Authentication authentication) {
        return notificationService.subscribe(authentication.getName());
    }
}
