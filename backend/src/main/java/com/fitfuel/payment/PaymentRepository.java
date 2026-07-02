package com.fitfuel.payment;

import com.fitfuel.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByUserOrderByCreatedAtDesc(AppUser user);
}
