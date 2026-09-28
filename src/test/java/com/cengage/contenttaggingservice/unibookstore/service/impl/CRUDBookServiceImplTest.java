package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.GetBooksRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import com.cengage.contenttaggingservice.unibookstore.mapper.BookMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

@ExtendWith(MockitoExtension.class)
class CRUDBookServiceImplTest {

    private static final Long BOOK_ID = 42L;

    @Mock
    private BookRepository repository;

    @Mock
    private BookMapper mapper;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CRUDBookServiceImpl crudBookService;

    private BookResponse sampleResponse() {
        return new BookResponse(BOOK_ID, "Dune", "desc", "Herbert",
                Genre.SCIENCE_FICTION, "img", true, 10, new BigDecimal("9.99"));
    }

    @Test
    void whenCreateBookThenBookAndStockPersistedFromRequest() {
        // Given
        CreateBookRequest request = new CreateBookRequest(
                "Dune", "desc", "Herbert", Genre.SCIENCE_FICTION, "img",
                true, new BigDecimal("9.99"), 10);
        BookResponse expectedResponse = sampleResponse();

        when(repository.save(any(Book.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Book.class)))
                .thenReturn(expectedResponse);

        // When
        BookResponse actualResponse = crudBookService.createBook(request);

        // Then
        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(repository, times(1)).save(bookCaptor.capture());
        Book savedBook = bookCaptor.getValue();

        assertThat(savedBook.getTitle()).isEqualTo("Dune");
        assertThat(savedBook.getDescription()).isEqualTo("desc");
        assertThat(savedBook.getAuthor()).isEqualTo("Herbert");
        assertThat(savedBook.getGenre()).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(savedBook.getImageUrl()).isEqualTo("img");
        assertThat(savedBook.isEnabled()).isTrue();

        BookStock savedStock = savedBook.getStock();
        assertThat(savedStock).as("stock must be attached to the book").isNotNull();
        assertThat(savedStock.getPrice()).isEqualByComparingTo("9.99");
        assertThat(savedStock.getQuantity()).isEqualTo(10);

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    void givenExistingBookWhenUpdateBookDetailsThenMergedAndPersisted() {
        // Given
        Book existingBook = Book.builder().id(BOOK_ID).title("Old").build();
        UpdateBookDetailsRequest request = new UpdateBookDetailsRequest(
                "New", "desc", "Herbert", Genre.FANTASY, "img", true);
        BookResponse expectedResponse = sampleResponse();

        when(repository.findById(BOOK_ID))
                .thenReturn(Optional.of(existingBook));
        when(repository.save(any(Book.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(existingBook))
                .thenReturn(expectedResponse);

        // When
        BookResponse actualResponse = crudBookService.updateBookDetails(BOOK_ID, request);

        // Then
        verify(mapper, times(1)).updateBook(request, existingBook);
        verify(repository, times(1)).save(existingBook);
        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    void givenUnknownBookWhenUpdateBookDetailsThenThrowsAndDoesNotSave() {
        // Given
        UpdateBookDetailsRequest request = new UpdateBookDetailsRequest(
                "New", "desc", "Herbert", Genre.FANTASY, "img", true);

        when(repository.findById(BOOK_ID))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> crudBookService.updateBookDetails(BOOK_ID, request))
                .isInstanceOf(NoSuchElementException.class);

        verify(repository, never()).save(any());
        verify(mapper, never()).updateBook(any(), any());
    }

    @Test
    void givenNonAdminWhenFindAllThenRestrictedToEnabledAndResultsMapped() {
        // Given
        GetBooksRequest request = new GetBooksRequest(
                null, null, null, false, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Book book = Book.builder().id(BOOK_ID).build();
        BookResponse expectedResponse = sampleResponse();

        when(securityUtils.isCurrentUserAdmin()).thenReturn(false);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(book)));
        when(mapper.toResponse(book)).thenReturn(expectedResponse);

        // When
        Page<BookResponse> actualPage = crudBookService.findAll(request, pageable);

        // Then
        verify(securityUtils, times(1)).isCurrentUserAdmin();
        verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
        assertThat(actualPage.getContent()).containsExactly(expectedResponse);
    }

    @Test
    void givenAdminWhenFindAllThenResultsMapped() {
        // Given
        GetBooksRequest request = new GetBooksRequest(
                "Dune", "Herbert", Genre.SCIENCE_FICTION, true,
                new BigDecimal("1.00"), new BigDecimal("50.00"));
        Pageable pageable = PageRequest.of(0, 20);
        Book book = Book.builder().id(BOOK_ID).build();
        BookResponse expectedResponse = sampleResponse();

        when(securityUtils.isCurrentUserAdmin()).thenReturn(true);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(book)));
        when(mapper.toResponse(book)).thenReturn(expectedResponse);

        // When
        Page<BookResponse> actualPage = crudBookService.findAll(request, pageable);

        // Then
        verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
        assertThat(actualPage.getContent()).containsExactly(expectedResponse);
    }
}
