package com.fitfuel.feedback;

import com.fitfuel.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserOrderByCreatedAtDesc(AppUser user);
}
