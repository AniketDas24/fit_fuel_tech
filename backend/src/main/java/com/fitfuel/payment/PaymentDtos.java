package com.fitfuel.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

final class PaymentDtos {
    private PaymentDtos() {
    }
}

record CreatePaymentRequest(Long orderId, @NotNull @Positive BigDecimal amount, PaymentProvider provider) {
}

record PaymentResponse(Long id, Long orderId, BigDecimal amount, PaymentEntityStatus status,
                       PaymentProvider provider, String gatewayReference, Instant createdAt) {
    static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder() == null ? null : payment.getOrder().getId(),
                payment.getAmount(), payment.getStatus(), payment.getProvider(), payment.getGatewayReference(),
                payment.getCreatedAt());
    }
}

record CreateRazorpayOrderRequest(@NotNull Long orderId) {
}

record RazorpayOrderResponse(String razorpayOrderId, String keyId, BigDecimal amount, String currency,
                             Long orderId, Long paymentId) {
}

record VerifyPaymentRequest(@NotNull Long paymentId, @NotBlank String razorpayOrderId,
                            @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {
}
