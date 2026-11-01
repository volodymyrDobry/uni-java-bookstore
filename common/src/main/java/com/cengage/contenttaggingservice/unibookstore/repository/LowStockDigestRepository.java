package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LowStockDigestRepository extends JpaRepository<LowStockDigest, Long> {

    // Fetch the items alongside each digest to avoid an N+1 when building responses.
    @EntityGraph(attributePaths = "items")
    List<LowStockDigest> findByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
