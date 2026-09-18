package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.StockRequest;
import com.cengage.contenttaggingservice.unibookstore.exception.NotFoundException;
import com.cengage.contenttaggingservice.unibookstore.mapper.BookMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;

import java.math.BigDecimal;
import java.util.ArrayList;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository books;
    private final BookStockRepository stock;
    private final BookMapper mapper;

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = mapper.toEntity(request);
        book.setEnabled(true);
        return response(books.save(book));
    }

    @Override
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = book(id);
        mapper.update(request, book);
        return response(book);
    }

    @Override
    @Transactional
    public BookResponse setEnabled(Long id, boolean enabled) {
        Book book = book(id);
        book.setEnabled(enabled);
        return response(book);
    }

    @Override
    public Page<BookResponse> available(String query, Genre genre, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return books.findAll(availableSpecification(query, genre, minPrice, maxPrice), pageable).map(this::response);
    }

    @Override
    public Page<BookResponse> findAll(Pageable pageable) {
        return books.findAll(pageable).map(this::response);
    }

    @Override
    @Transactional
    public BookResponse createOrUpdateStock(Long bookId, StockRequest request) {
        Book book = book(bookId);
        BookStock bookStock = stock.findByBookId(bookId).orElseGet(() -> BookStock.builder().book(book).build());
        bookStock.setQuantity(request.quantity());
        bookStock.setPrice(request.price());
        stock.save(bookStock);
        return response(book);
    }

    @Override
    @Transactional
    public BookResponse addStock(Long bookId, int quantity) {
        BookStock bookStock = stock(bookId);
        bookStock.setQuantity(bookStock.getQuantity() + quantity);
        return response(bookStock.getBook());
    }

    @Override
    @Transactional
    public BookResponse removeStock(Long bookId, int quantity) {
        BookStock bookStock = stock(bookId);
        if (bookStock.getQuantity() < quantity)
            throw new IllegalArgumentException("Cannot remove more books than are in stock");
        bookStock.setQuantity(bookStock.getQuantity() - quantity);
        return response(bookStock.getBook());
    }

    @Override
    @Transactional
    public BookResponse updatePrice(Long bookId, BigDecimal price) {
        BookStock bookStock = stock(bookId);
        bookStock.setPrice(price);
        return response(bookStock.getBook());
    }

    @Override
    public Book book(Long id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Book " + id + " was not found"));
    }

    @Override
    public void requireAvailable(Long bookId, int requestedQuantity) {
        Book book = book(bookId);
        if (!book.isEnabled() || stock(bookId).getQuantity() < requestedQuantity)
            throw new IllegalArgumentException("Requested book quantity is not available");
    }

    private BookStock stock(Long bookId) {
        return stock.findByBookId(bookId).orElseThrow(() -> new NotFoundException("Stock for book " + bookId + " was not found"));
    }

    private BookResponse response(Book book) {
        return stock.findByBookId(book.getId()).map(bookStock -> mapper.toResponse(book, bookStock)).orElseGet(() -> new BookResponse(book.getId(), book.getTitle(), book.getDescription(), book.getAuthor(), book.getGenre(), book.getImageUrl(), book.isEnabled(), 0, null));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Specification<Book> availableSpecification(String query, Genre genre, BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, criteriaQuery, builder) -> {
            Root<BookStock> stockRoot = criteriaQuery.from(BookStock.class);
            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isTrue(root.get("enabled")));
            predicates.add(builder.equal(stockRoot.get("book").get("id"), root.get("id")));
            predicates.add(builder.greaterThan(stockRoot.get("quantity"), 0));

            String searchTerm = blankToNull(query);
            if (searchTerm != null) {
                String pattern = "%" + searchTerm.toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            if (genre != null) predicates.add(builder.equal(root.get("genre"), genre));
            if (minPrice != null) predicates.add(builder.greaterThanOrEqualTo(stockRoot.get("price"), minPrice));
            if (maxPrice != null) predicates.add(builder.lessThanOrEqualTo(stockRoot.get("price"), maxPrice));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
