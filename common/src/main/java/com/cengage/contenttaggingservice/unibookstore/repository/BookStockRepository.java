package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookStockRepository extends JpaRepository<BookStock, Long> {

    BookStock findByBookId(Long bookId);

    @Query("select s from BookStock s join fetch s.book where s.quantity <= :threshold order by s.quantity asc")
    List<BookStock> findLowStock(@Param("threshold") int threshold);
}
