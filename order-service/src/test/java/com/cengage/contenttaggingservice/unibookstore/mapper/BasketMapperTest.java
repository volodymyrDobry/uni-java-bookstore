package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketItemResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BasketMapperTest {

    private final BasketMapper mapper = new BasketMapperImpl();

    private BookItem item(Long bookId, String title, int quantity) {
        return new BookItem(99L, quantity, Book.builder().id(bookId).title(title).build(), null);
    }

    @Test
    void toItemResponseFlattensBook() {
        BasketItemResponse response = mapper.toItemResponse(item(42L, "Dune", 2));

        assertThat(response).usingRecursiveComparison()
                .isEqualTo(new BasketItemResponse(42L, "Dune", 2));
    }

    @Test
    void toItemResponseHandlesNullBook() {
        BasketItemResponse response = mapper.toItemResponse(new BookItem(1L, 5, null, null));

        assertThat(response.bookId()).isNull();
        assertThat(response.title()).isNull();
        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    void toItemResponseReturnsNullForNull() {
        assertThat(mapper.toItemResponse(null)).isNull();
    }

    @Test
    void toBasketResponseMapsIdAndItems() {
        Set<BookItem> books = new LinkedHashSet<>();
        books.add(item(42L, "Dune", 2));
        Basket basket = new Basket(1L, "Vova", books);

        BasketResponse response = mapper.toBasketResponse(basket);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.items())
                .containsExactly(new BasketItemResponse(42L, "Dune", 2));
    }

    @Test
    void toBasketResponseReturnsNullForNull() {
        assertThat(mapper.toBasketResponse(null)).isNull();
    }

    @Test
    void toItemResponsesReturnsNullForNull() {
        assertThat(mapper.toItemResponses(null)).isNull();
    }
}
