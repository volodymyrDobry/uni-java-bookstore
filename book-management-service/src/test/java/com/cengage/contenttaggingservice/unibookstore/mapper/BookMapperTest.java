package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BookMapperTest {

    private final BookMapper mapper = new BookMapperImpl();

    @Test
    void toResponseMapsAllFieldsIncludingFlattenedStock() {
        Book book = Book.builder()
                .id(1L)
                .title("Dune")
                .description("desc")
                .author("Herbert")
                .genre(Genre.SCIENCE_FICTION)
                .imageUrl("img")
                .enabled(true)
                .stock(BookStock.builder().quantity(7).price(new BigDecimal("9.99")).build())
                .build();

        BookResponse response = mapper.toResponse(book);

        assertThat(response).usingRecursiveComparison().isEqualTo(
                new BookResponse(1L, "Dune", "desc", "Herbert",
                        Genre.SCIENCE_FICTION, "img", true, 7, new BigDecimal("9.99")));
    }

    @Test
    void toResponseLeavesStockFieldsNullWhenNoStock() {
        Book book = Book.builder().id(1L).title("Dune").enabled(false).build();

        BookResponse response = mapper.toResponse(book);

        assertThat(response.quantity()).isNull();
        assertThat(response.price()).isNull();
    }

    @Test
    void toResponseReturnsNullForNullInput() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void updateBookOverwritesProvidedFieldsAndKeepsIdAndStock() {
        BookStock stock = BookStock.builder().quantity(3).price(BigDecimal.ONE).build();
        Book book = Book.builder()
                .id(1L).title("Old").description("oldDesc").author("Old")
                .genre(Genre.FANTASY).imageUrl("oldImg").enabled(false).stock(stock).build();
        UpdateBookDetailsRequest request = new UpdateBookDetailsRequest(
                "New", "newDesc", "Herbert", Genre.SCIENCE_FICTION, "newImg", true);

        mapper.updateBook(request, book);

        assertThat(book.getId()).isEqualTo(1L);
        assertThat(book.getStock()).isSameAs(stock);
        assertThat(book.getTitle()).isEqualTo("New");
        assertThat(book.getDescription()).isEqualTo("newDesc");
        assertThat(book.getAuthor()).isEqualTo("Herbert");
        assertThat(book.getGenre()).isEqualTo(Genre.SCIENCE_FICTION);
        assertThat(book.getImageUrl()).isEqualTo("newImg");
        assertThat(book.isEnabled()).isTrue();
    }

    @Test
    void updateBookIgnoresNullFields() {
        Book book = Book.builder()
                .id(1L).title("Old").description("oldDesc").author("Old")
                .genre(Genre.FANTASY).imageUrl("oldImg").enabled(true).build();
        UpdateBookDetailsRequest request = new UpdateBookDetailsRequest(
                null, null, null, null, null, null);

        mapper.updateBook(request, book);

        assertThat(book.getTitle()).isEqualTo("Old");
        assertThat(book.getDescription()).isEqualTo("oldDesc");
        assertThat(book.getGenre()).isEqualTo(Genre.FANTASY);
        assertThat(book.isEnabled()).isTrue();
    }

    @Test
    void updateBookWithNullRequestIsNoOp() {
        Book book = Book.builder().id(1L).title("Old").build();
        mapper.updateBook(null, book);
        assertThat(book.getTitle()).isEqualTo("Old");
    }
}
