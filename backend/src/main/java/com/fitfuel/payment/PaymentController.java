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
    private final PaymentService paymentService;

    public PaymentController(PaymentRepository paymentRepository, OrderRepository orderRepository,
                             UserRepository userRepository, PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
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

    @PostMapping("/razorpay-order")
    RazorpayOrderResponse razorpayOrder(Authentication authentication,
                                        @Valid @RequestBody CreateRazorpayOrderRequest request) {
        return paymentService.createRazorpayOrder(authentication.getName(), request.orderId());
    }

    @PostMapping("/verify")
    PaymentResponse verify(Authentication authentication, @Valid @RequestBody VerifyPaymentRequest request) {
        return paymentService.verifyPayment(authentication.getName(), request);
    }

    // Called by Razorpay's servers (no JWT). Authenticity is enforced by the signature.
    @PostMapping("/webhook")
    void webhook(@RequestBody(required = false) String payload,
                 @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(payload == null ? "" : payload, signature == null ? "" : signature);
    }

    private AppUser user(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
