package com.fitfuel.subscription;

import com.fitfuel.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserAndStatusOrderByStartDateDesc(AppUser user, SubscriptionStatus status);

    List<Subscription> findByUserOrderByStartDateDesc(AppUser user);
}
