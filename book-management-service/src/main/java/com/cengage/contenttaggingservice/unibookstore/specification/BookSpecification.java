package com.cengage.contenttaggingservice.unibookstore.specification;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookSpecification {

    private BookSpecification() {
    }

    public static Specification<Book> from(GetBooksRequest request, boolean isEnabled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.title() != null && !request.title().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + request.title().toLowerCase() + "%"));
            }

            if (request.author() != null && !request.author().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("author")),
                        "%" + request.author().toLowerCase() + "%"));
            }

            if (request.genre() != null) {
                predicates.add(cb.equal(root.get("genre"), request.genre()));
            }

            predicates.add(cb.equal(root.get("enabled"), isEnabled));

            boolean needsStock = request.minPrice() != null || request.maxPrice() != null;
            if (needsStock) {
                Join<Book, BookStock> stock = root.join("stock");

                if (request.minPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(stock.get("price"), request.minPrice()));
                }
                if (request.maxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(stock.get("price"), request.maxPrice()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
