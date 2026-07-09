package com.fitfuel.order;

import com.fitfuel.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserOrderByCreatedAtDesc(AppUser user);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(Instant since);

    long countByStatusNotIn(List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o "
            + "WHERE o.createdAt >= :since AND o.paymentStatus = com.fitfuel.order.PaymentStatus.PAID")
    BigDecimal revenueSince(@Param("since") Instant since);
}
