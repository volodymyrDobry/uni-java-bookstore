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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository books;
    @Mock
    private BookStockRepository stock;
    @Mock
    private BookMapper mapper;
    @InjectMocks
    private BookServiceImpl service;
    @Captor
    private ArgumentCaptor<BookStock> stockCaptor;
    @Captor
    private ArgumentCaptor<Specification<Book>> specificationCaptor;

    @Test
    void createsEnabledBook() {
        BookRequest request = request();
        Book book = book(1L, true);
        when(mapper.toEntity(request)).thenReturn(book);
        when(books.save(book)).thenReturn(book);
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        BookResponse result = service.create(request);

        assertThat(book.isEnabled()).isTrue();
        assertThat(result.id()).isEqualTo(1L);
        verify(books).save(book);
    }

    @Test
    void updatesBookAndVisibility() {
        Book book = book(1L, true);
        when(books.findById(1L)).thenReturn(Optional.of(book));
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        service.update(1L, request());
        BookResponse result = service.setEnabled(1L, false);

        verify(mapper).update(request(), book);
        assertThat(result.enabled()).isFalse();
    }

    @Test
    void returnsAllBooks() {
        Book book = book(1L, true);
        when(books.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(book)));
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        assertThat(service.findAll(PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void createsAndUpdatesStock() {
        Book book = book(1L, true);
        StockRequest request = new StockRequest(7, BigDecimal.valueOf(14.50));
        when(books.findById(1L)).thenReturn(Optional.of(book));
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        service.createOrUpdateStock(1L, request);

        verify(stock).save(stockCaptor.capture());
        assertThat(stockCaptor.getValue().getBook()).isSameAs(book);
        assertThat(stockCaptor.getValue().getQuantity()).isEqualTo(7);
        assertThat(stockCaptor.getValue().getPrice()).isEqualByComparingTo("14.50");
    }

    @Test
    void changesStockAndPrice() {
        Book book = book(1L, true);
        BookStock bookStock = stock(book, 5, "10.00");
        BookResponse response = response(book, bookStock);
        when(stock.findByBookId(1L)).thenReturn(Optional.of(bookStock));
        when(mapper.toResponse(book, bookStock)).thenReturn(response);

        service.addStock(1L, 3);
        service.removeStock(1L, 2);
        BookResponse result = service.updatePrice(1L, BigDecimal.valueOf(12));

        assertThat(bookStock.getQuantity()).isEqualTo(6);
        assertThat(bookStock.getPrice()).isEqualByComparingTo("12");
        assertThat(result).isSameAs(response);
    }

    @Test
    void rejectsStockRemovalThatExceedsCurrentQuantity() {
        BookStock bookStock = stock(book(1L, true), 2, "10.00");
        when(stock.findByBookId(1L)).thenReturn(Optional.of(bookStock));

        assertThatThrownBy(() -> service.removeStock(1L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot remove");
    }

    @Test
    void reportsMissingBookAndStock() {
        when(books.findById(99L)).thenReturn(Optional.empty());
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(99L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.addStock(1L, 1)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void validatesBookAvailability() {
        Book available = book(1L, true);
        Book disabled = book(2L, false);
        when(books.findById(1L)).thenReturn(Optional.of(available));
        when(books.findById(2L)).thenReturn(Optional.of(disabled));
        when(stock.findByBookId(1L)).thenReturn(Optional.of(stock(available, 2, "10.00")));

        service.requireAvailable(1L, 2);

        assertThatThrownBy(() -> service.requireAvailable(1L, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.requireAvailable(2L, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void buildsAvailableBooksSpecificationForProvidedAndOmittedFilters() {
        PageRequest pageable = PageRequest.of(0, 10);
        Book book = book(1L, true);
        when(books.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(book)));
        when(stock.findByBookId(1L)).thenReturn(Optional.empty());

        service.available("Dune", Genre.SCIENCE_FICTION, BigDecimal.ONE, BigDecimal.TEN, pageable);
        verify(books).findAll(specificationCaptor.capture(), eq(pageable));
        invokeSpecification(specificationCaptor.getValue());

        service.available(" ", null, null, null, pageable);
        verify(books, org.mockito.Mockito.times(2)).findAll(specificationCaptor.capture(), eq(pageable));
        invokeSpecification(specificationCaptor.getAllValues().get(1));
    }

    private void invokeSpecification(Specification<Book> specification) {
        Root<Book> bookRoot = mock(Root.class);
        Root<BookStock> stockRoot = mock(Root.class);
        Path<Object> bookPath = mock(Path.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        when(query.from(BookStock.class)).thenReturn((Root) stockRoot);
        when(stockRoot.get("book")).thenReturn((Path) bookPath);
        specification.toPredicate(bookRoot, query, builder);
    }

    private BookRequest request() {
        return new BookRequest("Dune", "Desert planet", "Frank Herbert", Genre.SCIENCE_FICTION, "image");
    }

    private Book book(Long id, boolean enabled) {
        return Book.builder().id(id).title("Dune").description("Desert planet").author("Frank Herbert").genre(Genre.SCIENCE_FICTION).imageUrl("image").enabled(enabled).build();
    }

    private BookStock stock(Book book, int quantity, String price) {
        return BookStock.builder().id(1L).book(book).quantity(quantity).price(new BigDecimal(price)).build();
    }

    private BookResponse response(Book book, BookStock stock) {
        return new BookResponse(book.getId(), book.getTitle(), book.getDescription(), book.getAuthor(), book.getGenre(), book.getImageUrl(), book.isEnabled(), stock.getQuantity(), stock.getPrice());
    }
}
