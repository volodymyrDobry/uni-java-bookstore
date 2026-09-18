package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookStockRepository extends JpaRepository<BookStock, Long> {
    Optional<BookStock> findByBookId(Long bookId);
}
