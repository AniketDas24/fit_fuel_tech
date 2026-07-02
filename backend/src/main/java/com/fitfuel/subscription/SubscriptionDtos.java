package com.fitfuel.subscription;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

final class SubscriptionDtos {
    private SubscriptionDtos() {
    }
}

record CreateSubscriptionRequest(
        @NotNull PlanType planType,
        @NotNull ProteinTier proteinTier,
        @NotNull MealType mealType,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull LocalDate endDate
) {
}

record SubscriptionResponse(Long id, PlanType planType, ProteinTier proteinTier, MealType mealType,
                            LocalDate startDate, LocalDate endDate, SubscriptionStatus status) {
    static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(subscription.getId(), subscription.getPlanType(), subscription.getProteinTier(),
                subscription.getMealType(), subscription.getStartDate(), subscription.getEndDate(),
                subscription.getStatus());
    }
}
