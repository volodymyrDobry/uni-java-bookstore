package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface OrderingRepository extends JpaRepository<Ordering, Long> {

    List<Ordering> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Ordering> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);
}
