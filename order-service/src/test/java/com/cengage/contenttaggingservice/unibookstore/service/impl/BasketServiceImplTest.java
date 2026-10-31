package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import com.cengage.contenttaggingservice.unibookstore.mapper.BasketMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BasketRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceImplTest {

    private static final String USERNAME = "Vova";
    private static final Long BASKET_ID = 1L;
    private static final Long BOOK_ID = 42L;
    private static final Long BOOK_ITEM_ID = 999L;

    @Mock
    private BasketRepository basketRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private BasketMapper basketMapper;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BasketServiceImpl basketService;

    private void givenCurrentUser() {
        when(securityUtils.getCurrentUser())
                .thenReturn(new CurrentUser(USERNAME, List.of()));
    }

    private BookItem bookItem(Long itemId, Long bookId, int quantity, Basket basket) {
        return new BookItem(itemId, quantity, Book.builder().id(bookId).build(), basket);
    }

    @Test
    void whenGetUserBasketThenReturnNewBasketCreatedForCurrentUser() {
        // Given
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.empty());
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        BasketResponse actualResponse = basketService.getUsersBasket(USERNAME);

        // Then
        BasketResponse expectedResponse = new BasketResponse(BASKET_ID, List.of());
        Basket expectedSavedBasket = new Basket(null, USERNAME, Set.of());

        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
        assertThat(basketCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedSavedBasket);
    }

    @Test
    void whenGetUserBasketThenReturnExistingBasketForCurrentUser() {
        // Given
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(Basket.builder().id(BASKET_ID).build()));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        BasketResponse actualResponse = basketService.getUsersBasket(USERNAME);

        // Then
        BasketResponse expectedResponse = new BasketResponse(BASKET_ID, List.of());

        verify(basketRepository, never()).save(any());
        assertThat(actualResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    void givenBookNotInBasketWhenAddBookToTheBasketThenNewItemAdded() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        Book book = Book.builder().id(BOOK_ID).build();
        UpdateBasketItemRequest request = new UpdateBasketItemRequest(BOOK_ID, 3);

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(bookRepository.getReferenceById(BOOK_ID))
                .thenReturn(book);
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        basketService.addBookToTheBasket(request);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        Set<BookItem> savedItems = basketCaptor.getValue().getBooks();
        assertThat(savedItems).hasSize(1);
        BookItem added = savedItems.iterator().next();
        assertThat(added.getQuantity()).isEqualTo(3);
        assertThat(added.getBook().getId()).isEqualTo(BOOK_ID);
    }

    @Test
    void givenBookAlreadyInBasketWhenAddBookToTheBasketThenQuantityIncremented() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        basket.getBooks().add(bookItem(BOOK_ITEM_ID, BOOK_ID, 2, basket));
        Book book = Book.builder().id(BOOK_ID).build();
        UpdateBasketItemRequest request = new UpdateBasketItemRequest(BOOK_ID, 3);

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(bookRepository.getReferenceById(BOOK_ID))
                .thenReturn(book);
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        basketService.addBookToTheBasket(request);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        Set<BookItem> savedItems = basketCaptor.getValue().getBooks();
        assertThat(savedItems)
                .as("adding an already-present book must not create a duplicate item")
                .hasSize(1);
        BookItem item = savedItems.iterator().next();
        assertThat(item.getBook().getId()).isEqualTo(BOOK_ID);
        assertThat(item.getQuantity())
                .as("quantity should be incremented from 2 by 3")
                .isEqualTo(5);
    }

    @Test
    void givenMatchingBookWhenUpdateBasketItemsThenQuantityReplaced() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        basket.getBooks().add(bookItem(BOOK_ITEM_ID, BOOK_ID, 2, basket));
        List<UpdateBasketItemRequest> request = List.of(new UpdateBasketItemRequest(BOOK_ID, 7));

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        basketService.updateBasketItems(request);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        BookItem item = basketCaptor.getValue().getBooks().iterator().next();
        assertThat(item.getBook().getId()).isEqualTo(BOOK_ID);
        assertThat(item.getQuantity())
                .as("quantity should be replaced with the requested value")
                .isEqualTo(7);
    }

    @Test
    void givenNonMatchingBookWhenUpdateBasketItemsThenQuantityUnchanged() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        basket.getBooks().add(bookItem(BOOK_ITEM_ID, BOOK_ID, 2, basket));
        Long otherBookId = BOOK_ID + 1;
        List<UpdateBasketItemRequest> request = List.of(new UpdateBasketItemRequest(otherBookId, 7));

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(basketMapper.toBasketResponse(any(Basket.class)))
                .thenReturn(new BasketResponse(BASKET_ID, List.of()));

        // When
        basketService.updateBasketItems(request);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        BookItem item = basketCaptor.getValue().getBooks().iterator().next();
        assertThat(item.getQuantity())
                .as("a book not present in the basket must not affect existing items")
                .isEqualTo(2);
    }

    @Test
    void givenBookInBasketWhenRemoveBookFromBasketThenItemRemoved() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        basket.getBooks().add(bookItem(BOOK_ITEM_ID, BOOK_ID, 2, basket));

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        basketService.removeBookFromBasket(BOOK_ID);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository, times(1)).save(basketCaptor.capture());

        assertThat(basketCaptor.getValue().getBooks())
                .as("the item holding the requested book id should be removed")
                .isEmpty();
    }

    @Test
    void givenBookNotInBasketWhenRemoveBookFromBasketThenNothingRemoved() {
        // Given
        Basket basket = new Basket(BASKET_ID, USERNAME, new HashSet<>());
        basket.getBooks().add(bookItem(BOOK_ITEM_ID, BOOK_ID, 2, basket));

        givenCurrentUser();
        when(basketRepository.findByUserId(USERNAME))
                .thenReturn(Optional.of(basket));
        when(basketRepository.save(any(Basket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        basketService.removeBookFromBasket(BOOK_ID + 1);

        // Then
        ArgumentCaptor<Basket> basketCaptor = ArgumentCaptor.forClass(Basket.class);
        verify(basketRepository).save(basketCaptor.capture());

        assertThat(basketCaptor.getValue().getBooks())
                .as("removing an absent book id should leave the basket untouched")
                .hasSize(1);
    }
}
