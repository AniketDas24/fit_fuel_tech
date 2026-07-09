package com.fitfuel.admin;

import com.fitfuel.order.OrderRepository;
import com.fitfuel.order.OrderStatus;
import com.fitfuel.user.Role;
import com.fitfuel.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    AdminStatsResponse stats() {
        // "Today" uses the server's default timezone — a known simplification, not timezone-aware reporting.
        Instant since = Instant.now().truncatedTo(ChronoUnit.DAYS);
        return new AdminStatsResponse(
                orderRepository.countByCreatedAtAfter(since),
                orderRepository.revenueSince(since),
                orderRepository.countByStatusNotIn(List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)),
                userRepository.countByRole(Role.CUSTOMER)
        );
    }
}
