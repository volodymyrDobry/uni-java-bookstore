package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookStockRepository extends JpaRepository<BookStock, Long> {
    BookStock findByBookId(Long bookId);
}
