package com.fitfuel.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

final class FeedbackDtos {
    private FeedbackDtos() {
    }
}

record CreateFeedbackRequest(@NotNull Long orderId, @Min(1) @Max(5) int rating, String comment) {
}

record FeedbackResponse(Long id, Long orderId, int rating, String comment, Instant createdAt) {
    static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(feedback.getId(), feedback.getOrder().getId(), feedback.getRating(),
                feedback.getComment(), feedback.getCreatedAt());
    }
}

record AdminFeedbackResponse(Long id, Long orderId, String userName, int rating, String comment, Instant createdAt) {
    static AdminFeedbackResponse from(Feedback feedback) {
        return new AdminFeedbackResponse(feedback.getId(), feedback.getOrder().getId(), feedback.getUser().getName(),
                feedback.getRating(), feedback.getComment(), feedback.getCreatedAt());
    }
}
