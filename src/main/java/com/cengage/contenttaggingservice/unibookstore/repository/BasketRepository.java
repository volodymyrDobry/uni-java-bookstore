package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BasketRepository extends JpaRepository<Basket, Long> {
    Optional<Basket> findByUserId(String userId);
}
