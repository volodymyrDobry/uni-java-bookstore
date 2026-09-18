package com.cengage.contenttaggingservice.unibookstore.repository;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookItemRepository extends JpaRepository<BookItem, Long> {
    List<BookItem> findAllByBasketId(Long basketId);

    Optional<BookItem> findByBasketIdAndBookId(Long basketId, Long bookId);

    void deleteByBasketIdAndBookId(Long basketId, Long bookId);
}
