package com.fitfuel.feedback;

import com.fitfuel.common.NotFoundException;
import com.fitfuel.order.OrderRepository;
import com.fitfuel.user.AppUser;
import com.fitfuel.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public FeedbackController(FeedbackRepository feedbackRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    FeedbackResponse create(Authentication authentication, @Valid @RequestBody CreateFeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setUser(user(authentication));
        feedback.setOrder(orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Order not found")));
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @GetMapping
    List<FeedbackResponse> list(Authentication authentication) {
        return feedbackRepository.findByUserOrderByCreatedAtDesc(user(authentication))
                .stream()
                .map(FeedbackResponse::from)
                .toList();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    List<AdminFeedbackResponse> allFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream().map(AdminFeedbackResponse::from).toList();
    }

    private AppUser user(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
