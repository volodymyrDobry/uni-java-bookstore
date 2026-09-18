package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketItemResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.mapper.BasketMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BasketRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.BookItemRepository;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceImplTest {

    @Mock
    private BasketRepository baskets;
    @Mock
    private BookItemRepository items;
    @Mock
    private BookService books;
    @Mock
    private BasketMapper mapper;
    @InjectMocks
    private BasketServiceImpl service;
    @Captor
    private ArgumentCaptor<BookItem> itemCaptor;

    @Test
    void addsNewBookToNewBasket() {
        Basket basket = Basket.builder().id(1L).userId("alice").build();
        Book book = book(10L);
        when(books.book(10L)).thenReturn(book);
        when(baskets.findByUserId("alice")).thenReturn(Optional.empty());
        when(baskets.save(org.mockito.ArgumentMatchers.any(Basket.class))).thenReturn(basket);
        when(items.findByBasketIdAndBookId(1L, 10L)).thenReturn(Optional.empty());
        when(items.findAllByBasketId(1L)).thenReturn(List.of());

        BasketResponse response = service.add("alice", 10L, 2);

        verify(books).requireAvailable(10L, 2);
        verify(items).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getBasket()).isSameAs(basket);
        assertThat(itemCaptor.getValue().getBook()).isSameAs(book);
        assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(2);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void incrementsExistingBasketItem() {
        Basket basket = basket();
        Book book = book(10L);
        BookItem item = BookItem.builder().id(2L).basket(basket).book(book).quantity(2).build();
        when(books.book(10L)).thenReturn(book);
        when(baskets.findByUserId("alice")).thenReturn(Optional.of(basket));
        when(items.findByBasketIdAndBookId(1L, 10L)).thenReturn(Optional.of(item));
        when(items.findAllByBasketId(1L)).thenReturn(List.of());

        service.add("alice", 10L, 3);

        assertThat(item.getQuantity()).isEqualTo(5);
        verify(books).requireAvailable(10L, 5);
        verify(baskets, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getsBasketAndMapsItems() {
        Basket basket = basket();
        BookItem item = BookItem.builder().id(2L).basket(basket).book(book(10L)).quantity(2).build();
        BasketItemResponse itemResponse = new BasketItemResponse(10L, "Dune", 2);
        when(baskets.findByUserId("alice")).thenReturn(Optional.of(basket));
        when(items.findAllByBasketId(1L)).thenReturn(List.of(item));
        when(mapper.toResponse(item)).thenReturn(itemResponse);

        BasketResponse response = service.get("alice");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.items()).containsExactly(itemResponse);
    }

    @Test
    void removesBookFromExistingBasket() {
        when(baskets.findByUserId("alice")).thenReturn(Optional.of(basket()));

        service.remove("alice", 10L);

        verify(items).deleteByBasketIdAndBookId(1L, 10L);
    }

    @Test
    void changesQuantityForExistingItem() {
        Basket basket = basket();
        BookItem item = BookItem.builder().id(2L).basket(basket).book(book(10L)).quantity(1).build();
        when(baskets.findByUserId("alice")).thenReturn(Optional.of(basket));
        when(items.findByBasketIdAndBookId(1L, 10L)).thenReturn(Optional.of(item));
        when(items.findAllByBasketId(1L)).thenReturn(List.of());

        service.changeQuantity("alice", 10L, 4);

        assertThat(item.getQuantity()).isEqualTo(4);
        verify(books).requireAvailable(10L, 4);
    }

    @Test
    void rejectsQuantityChangeForBookThatIsNotInBasket() {
        when(baskets.findByUserId("alice")).thenReturn(Optional.of(basket()));
        when(items.findByBasketIdAndBookId(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeQuantity("alice", 10L, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in basket");
    }

    private Basket basket() {
        return Basket.builder().id(1L).userId("alice").build();
    }

    private Book book(Long id) {
        return Book.builder().id(id).title("Dune").description("Desert planet").author("Frank Herbert").genre(Genre.SCIENCE_FICTION).imageUrl("image").enabled(true).build();
    }
}
