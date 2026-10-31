package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CRUDBookService {

    BookResponse createBook(CreateBookRequest request);

    List<BookResponse> createBooks(List<CreateBookRequest> requests);

    BookResponse updateBookDetails(Long id, UpdateBookDetailsRequest request);

    Page<BookResponse> findAll(GetBooksRequest getBooksRequest, Pageable pageable);

    BookResponse findOne(Long id);
}
