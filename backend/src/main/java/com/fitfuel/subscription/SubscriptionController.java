package com.fitfuel.subscription;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public SubscriptionController(SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    SubscriptionResponse create(Authentication authentication, @Valid @RequestBody CreateSubscriptionRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        Subscription subscription = new Subscription();
        subscription.setUser(user(authentication));
        subscription.setPlanType(request.planType());
        subscription.setProteinTier(request.proteinTier());
        subscription.setMealType(request.mealType());
        subscription.setStartDate(request.startDate());
        subscription.setEndDate(request.endDate());
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @GetMapping
    List<SubscriptionResponse> list(Authentication authentication) {
        return subscriptionRepository.findByUserOrderByStartDateDesc(user(authentication))
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @GetMapping("/active")
    SubscriptionResponse active(Authentication authentication) {
        return subscriptionRepository.findFirstByUserAndStatusOrderByStartDateDesc(user(authentication), SubscriptionStatus.ACTIVE)
                .map(SubscriptionResponse::from)
                .orElseThrow(() -> new NotFoundException("Active subscription not found"));
    }

    private AppUser user(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
