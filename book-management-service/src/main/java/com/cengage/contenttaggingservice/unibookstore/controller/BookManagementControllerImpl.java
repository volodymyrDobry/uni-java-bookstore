package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.BookManagementController;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import com.cengage.contenttaggingservice.unibookstore.service.CRUDBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookManagementControllerImpl implements BookManagementController {

    private final CRUDBookService bookService;

    @Override
    public BookResponse createBook(CreateBookRequest request) {
        return bookService.createBook(request);
    }

    @Override
    public BookResponse updateBookDetails(Long id, UpdateBookDetailsRequest request) {
        return bookService.updateBookDetails(id, request);
    }

    @Override
    public Page<BookResponse> findAllBooks(GetBooksRequest getBooksRequest, Pageable pageable) {
        return bookService.findAll(getBooksRequest, pageable);
    }
}
