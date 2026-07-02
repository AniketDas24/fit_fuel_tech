package com.fitfuel.payment;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.order.CustomerOrder;
import com.fitfuel.order.OrderRepository;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PaymentController(PaymentRepository paymentRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    PaymentResponse create(Authentication authentication, @Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = new Payment();
        AppUser user = user(authentication);
        payment.setUser(user);
        payment.setAmount(request.amount());
        payment.setProvider(request.provider() == null ? PaymentProvider.RAZORPAY : request.provider());
        if (request.orderId() != null) {
            CustomerOrder order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new NotFoundException("Order not found"));
            payment.setOrder(order);
        }
        payment.setGatewayReference("mock_" + System.currentTimeMillis());
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @GetMapping
    List<PaymentResponse> list(Authentication authentication) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user(authentication))
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private AppUser user(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
