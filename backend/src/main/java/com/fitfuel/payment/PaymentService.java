package com.fitfuel.payment;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.order.CustomerOrder;
import com.fitfuel.order.OrderRepository;
import com.fitfuel.order.OrderService;
import com.fitfuel.order.PaymentStatus;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          UserRepository userRepository, OrderService orderService,
                          @Value("${fitfuel.razorpay.key-id:}") String keyId,
                          @Value("${fitfuel.razorpay.key-secret:}") String keySecret,
                          @Value("${fitfuel.razorpay.webhook-secret:}") String webhookSecret) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(String email, Long orderId) {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay is not configured — set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        AppUser user = user(email);
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Order does not belong to the current user");
        }
        if (order.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Order is not awaiting payment");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + order.getId());
            Order razorpayOrder = client.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setOrder(order);
            payment.setAmount(order.getTotalAmount());
            payment.setProvider(PaymentProvider.RAZORPAY);
            payment.setGatewayReference(razorpayOrderId);
            Payment saved = paymentRepository.save(payment);

            return new RazorpayOrderResponse(razorpayOrderId, keyId, order.getTotalAmount(), "INR",
                    order.getId(), saved.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create Razorpay order: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(String email, VerifyPaymentRequest request) {
        AppUser user = user(email);
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Payment does not belong to the current user");
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.razorpayOrderId());
        options.put("razorpay_payment_id", request.razorpayPaymentId());
        options.put("razorpay_signature", request.razorpaySignature());

        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(options, keySecret);
        } catch (Exception e) {
            valid = false;
        }

        Long orderId = payment.getOrder() == null ? null : payment.getOrder().getId();
        if (valid) {
            payment.setStatus(PaymentEntityStatus.SUCCESS);
            payment.setGatewayReference(request.razorpayPaymentId());
            paymentRepository.save(payment);
            if (orderId != null) {
                orderService.confirmPayment(orderId);
            }
            return PaymentResponse.from(payment);
        }

        payment.setStatus(PaymentEntityStatus.FAILED);
        paymentRepository.save(payment);
        if (orderId != null) {
            orderService.markPaymentFailed(orderId);
        }
        throw new IllegalArgumentException("Payment verification failed");
    }

    // Server-to-server fallback: Razorpay calls this even if the customer's browser
    // dropped before /verify ran, so a paid order is never left stuck in PENDING.
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (webhookSecret.isBlank()) {
            log.warn("Razorpay webhook received but RAZORPAY_WEBHOOK_SECRET is not set — ignoring.");
            return;
        }
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (Exception e) {
            valid = false;
        }
        if (!valid) {
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String razorpayOrderId = extractOrderId(event);
        if (razorpayOrderId == null) {
            return; // an event we don't act on
        }
        // The Payment row's gatewayReference is the Razorpay order id until /verify runs.
        for (Payment payment : paymentRepository.findByGatewayReference(razorpayOrderId)) {
            if (payment.getStatus() == PaymentEntityStatus.SUCCESS) {
                continue; // already processed (idempotent)
            }
            payment.setStatus(PaymentEntityStatus.SUCCESS);
            paymentRepository.save(payment);
            if (payment.getOrder() != null) {
                try {
                    orderService.confirmPayment(payment.getOrder().getId());
                } catch (Exception e) {
                    log.warn("Webhook could not confirm order {}: {}", payment.getOrder().getId(), e.getMessage());
                }
            }
        }
    }

    private String extractOrderId(JSONObject event) {
        String type = event.optString("event", "");
        try {
            JSONObject payload = event.getJSONObject("payload");
            if (type.equals("payment.captured") || type.equals("payment.authorized")) {
                return payload.getJSONObject("payment").getJSONObject("entity").optString("order_id", null);
            }
            if (type.equals("order.paid")) {
                return payload.getJSONObject("order").getJSONObject("entity").optString("id", null);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private AppUser user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
