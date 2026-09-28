package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import com.cengage.contenttaggingservice.unibookstore.service.CRUDBookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookManagementControllerImplTest {

    @Mock
    private CRUDBookService bookService;

    @InjectMocks
    private BookManagementControllerImpl controller;

    private BookResponse response() {
        return new BookResponse(1L, "Dune", "d", "H",
                Genre.SCIENCE_FICTION, "img", true, 10, BigDecimal.ONE);
    }

    @Test
    void createBookDelegatesToService() {
        CreateBookRequest request = new CreateBookRequest(
                "Dune", "d", "H", Genre.SCIENCE_FICTION, "img", true, BigDecimal.ONE, 10);
        BookResponse expected = response();
        when(bookService.createBook(request)).thenReturn(expected);

        assertThat(controller.createBook(request)).isSameAs(expected);
    }

    @Test
    void updateBookDetailsDelegatesToService() {
        UpdateBookDetailsRequest request = new UpdateBookDetailsRequest(
                "Dune", "d", "H", Genre.FANTASY, "img", true);
        BookResponse expected = response();
        when(bookService.updateBookDetails(1L, request)).thenReturn(expected);

        assertThat(controller.updateBookDetails(1L, request)).isSameAs(expected);
    }

    @Test
    void findAllBooksDelegatesToService() {
        GetBooksRequest request = new GetBooksRequest(null, null, null, false, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<BookResponse> expected = new PageImpl<>(List.of(response()));
        when(bookService.findAll(request, pageable)).thenReturn(expected);

        assertThat(controller.findAllBooks(request, pageable)).isSameAs(expected);
    }
}
