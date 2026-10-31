package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.annotation.TimeTracker;
import com.cengage.contenttaggingservice.unibookstore.annotation.Auditable;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import com.cengage.contenttaggingservice.unibookstore.exception.NotFoundException;
import com.cengage.contenttaggingservice.unibookstore.mapper.BookMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.service.CRUDBookService;
import com.cengage.contenttaggingservice.unibookstore.specification.BookSpecification;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CRUDBookServiceImpl implements CRUDBookService {

    private final BookRepository repository;
    private final BookMapper mapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    @Auditable("BOOK_CREATED")
    public BookResponse createBook(CreateBookRequest request) {
        BookStock stock = BookStock.builder()
                .price(request.price())
                .quantity(request.quantity())
                .build();

        Book createdBook = Book.builder()
                .title(request.title())
                .description(request.description())
                .author(request.author())
                .genre(request.genre())
                .imageUrl(request.imageUrl())
                .enabled(request.enabled())
                .creatorId(securityUtils.getCurrentUser().username())
                .build();

        createdBook.assignStock(stock);

        return mapper.toResponse(repository.save(createdBook));
    }

    @Override
    @Transactional
    @Auditable("BOOKS_BULK_CREATED")
    public List<BookResponse> createBooks(List<CreateBookRequest> requests) {
        return requests.stream()
                .map(this::createBook)
                .toList();
    }

    @Override
    @TimeTracker
    @Transactional
    @Auditable("BOOK_UPDATED")
    public BookResponse updateBookDetails(Long id, UpdateBookDetailsRequest request) {
        Book bookById = repository.findById(id).orElseThrow();
        mapper.updateBook(request, bookById);
        return mapper.toResponse(repository.save(bookById));
    }

    @Override
    public Page<BookResponse> findAll(GetBooksRequest getBooksRequest, Pageable pageable) {
        boolean isEnabled = !securityUtils.isCurrentUserAdmin() || getBooksRequest.enabled();
        Specification<Book> specification = BookSpecification.from(getBooksRequest, isEnabled);
        return repository.findAll(specification, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public BookResponse findOne(Long id) {
        return repository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Book not found"));
    }

}
