package com.fitfuel.notification;

import com.fitfuel.order.CustomerOrder;
import com.fitfuel.order.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final long EMITTER_TIMEOUT_MS = 0L; // no timeout — connection stays open until closed/errored

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emittersByEmail = new ConcurrentHashMap<>();
    private final WhatsAppSender whatsAppSender;

    public NotificationService(WhatsAppSender whatsAppSender) {
        this.whatsAppSender = whatsAppSender;
    }

    public SseEmitter subscribe(String email) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByEmail.computeIfAbsent(email, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        return emitter;
    }

    public void notifyOrderStatusChanged(CustomerOrder order) {
        pushSse(order);
        sendWhatsApp(order);
    }

    private void pushSse(CustomerOrder order) {
        String email = order.getUser().getEmail();
        List<SseEmitter> emitters = emittersByEmail.get(email);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        OrderStatusEvent event = new OrderStatusEvent(order.getId(), order.getStatus().name());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-status")
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    private void sendWhatsApp(CustomerOrder order) {
        String phone = order.getUser().getPhone();
        if (phone == null || phone.isBlank()) {
            return;
        }
        String normalized = normalizePhone(phone);
        String message = buildMessage(order);
        try {
            whatsAppSender.send(normalized, message, buildTemplateVariables(order));
        } catch (Exception e) {
            // Never let a notification failure affect the order-status transaction that triggered it.
            log.warn("Failed to notify user {} about order {}: {}", order.getUser().getEmail(), order.getId(), e.getMessage());
        }
    }

    // Variables for the production WhatsApp template. Keys match {{1}}..{{4}} in the
    // approved template body (see DEPLOYMENT.md for the exact body to register).
    // Values are single-line (Meta disallows newlines inside template variables).
    private Map<String, String> buildTemplateVariables(CustomerOrder order) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", firstName(order.getUser().getName()));
        vars.put("2", String.valueOf(order.getId()));
        vars.put("3", statusLabel(order.getStatus()));
        vars.put("4", dishSummary(order));
        return vars;
    }

    private String statusLabel(OrderStatus status) {
        return switch (status) {
            case CREATED -> "Received";
            case CONFIRMED -> "Confirmed";
            case PREPARING -> "Being Prepared";
            case OUT_FOR_DELIVERY -> "Out for Delivery";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }

    // Builds a friendly, on-brand WhatsApp message per order status, including the dish name(s).
    // Note: in production template mode the whole string is passed as template variable {{1}};
    // Meta disallows newlines in variables, so a production template should instead be authored
    // with the static copy in the template body and only dynamic values passed in.
    private String buildMessage(CustomerOrder order) {
        String name = firstName(order.getUser().getName());
        String dishes = dishSummary(order);
        long id = order.getId();
        return switch (order.getStatus()) {
            case CONFIRMED -> "✨ Thank you, " + name + ".\n\n"
                    + "Your FitFuel order is confirmed, and our chefs will now prepare it with the utmost care.\n\n"
                    + "🍽️ " + dishes + "\nTotal: ₹" + order.getTotalAmount() + "\n\n"
                    + "Order #" + id + " · FitFuel";
            case PREPARING -> "👨‍🍳 " + name + ", your meal is now being freshly prepared.\n\n"
                    + dishes + "\n\n"
                    + "Crafted to order — a moment of patience for something worth savouring.\nOrder #" + id + " · FitFuel";
            case OUT_FOR_DELIVERY -> "🚗 " + name + ", your meal is on its way.\n\n"
                    + dishes + "\n\n"
                    + "Arriving shortly, prepared just for you. We hope you enjoy every bite.\nOrder #" + id + " · FitFuel";
            case DELIVERED -> "🍽️ Delivered with care, " + name + ".\n\n"
                    + "Your " + dishes + " has arrived. Bon appétit — here's to your health and wellbeing.\n\n"
                    + "With gratitude,\nThe FitFuel Team · Order #" + id;
            case CANCELLED -> name + ", your order #" + id + " has been cancelled.\n\n"
                    + "Should you need anything at all, our team is always here to assist you.\n— The FitFuel Team";
            case CREATED -> "Thank you, " + name + ". We've received your order.\n\n"
                    + dishes + "\n\n"
                    + "Kindly complete your payment to confirm. Order #" + id + " · FitFuel";
        };
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "there";
        }
        return fullName.trim().split("\\s+")[0];
    }

    private String dishSummary(CustomerOrder order) {
        String summary = order.getItems().stream()
                .map(item -> item.getQuantity() + "× " + item.getFoodItem().getName())
                .collect(java.util.stream.Collectors.joining(", "));
        return summary.isBlank() ? "your meal" : summary;
    }

    // AppUser.phone is free-text with no E.164 validation at signup. This assumes an India-only
    // user base (the app is priced in INR) — flagged as a pragmatic default, not a guarantee.
    private String normalizePhone(String rawPhone) {
        String digits = rawPhone.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+")) {
            return digits;
        }
        if (digits.length() == 10) {
            return "+91" + digits;
        }
        return "+" + digits;
    }
}
