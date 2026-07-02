package com.fitfuel.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

final class OrderDtos {
    private OrderDtos() {
    }
}

record AddCartItemRequest(@NotNull Long foodItemId, @Positive int quantity) {
}

record CartItemResponse(Long foodItemId, String name, int quantity, BigDecimal price) {
}

record CartResponse(Long id, List<CartItemResponse> items, BigDecimal totalAmount) {
}

record CheckoutRequest(String couponCode) {
}

record OrderItemResponse(Long foodItemId, String name, int quantity, BigDecimal price) {
}

record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, PaymentStatus paymentStatus,
                     Instant createdAt, List<OrderItemResponse> items) {
    static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(item.getFoodItem().getId(), item.getFoodItem().getName(),
                                item.getQuantity(), item.getPrice()))
                        .toList()
        );
    }
}
