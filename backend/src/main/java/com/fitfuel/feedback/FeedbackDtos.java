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
