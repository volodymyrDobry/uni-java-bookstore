package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderingRepository extends JpaRepository<Ordering, Long> {

    List<Ordering> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Ordering> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant from, Instant to);

    @Query("select o.id from Ordering o "
            + "where o.status not in :terminalStatuses "
            + "and o.createdAt <= :cutoff and o.receiptAttempts < :maxAttempts "
            + "order by o.createdAt asc")
    List<Long> findRecoverableIds(@Param("terminalStatuses") Collection<OrderStatus> terminalStatuses,
                                  @Param("cutoff") Instant cutoff,
                                  @Param("maxAttempts") int maxAttempts,
                                  Pageable pageable);
}
