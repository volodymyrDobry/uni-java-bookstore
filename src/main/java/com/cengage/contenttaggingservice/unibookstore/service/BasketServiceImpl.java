package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketItemResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.mapper.BasketMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BasketRepository;
import com.cengage.contenttaggingservice.unibookstore.repository.BookItemRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BasketServiceImpl implements BasketService {
    private final BasketRepository baskets;
    private final BookItemRepository items;
    private final BookService books;
    private final BasketMapper mapper;

    @Override
    @Transactional
    public BasketResponse add(String userId, Long bookId, int quantity) {
        Book book = books.book(bookId);
        Basket basket = findBasketByUserId(userId);
        BookItem item = items.findByBasketIdAndBookId(basket.getId(), bookId).orElseGet(() -> BookItem.builder().basket(basket).book(book).quantity(0).build());
        books.requireAvailable(bookId, item.getQuantity() + quantity);
        item.setQuantity(item.getQuantity() + quantity);
        items.save(item);
        return toBasketResponse(basket);
    }

    @Override
    public BasketResponse get(String userId) {
        return toBasketResponse(findBasketByUserId(userId));
    }

    @Override
    @Transactional
    public void remove(String userId, Long bookId) {
        items.deleteByBasketIdAndBookId(findBasketByUserId(userId).getId(), bookId);
    }

    @Override
    @Transactional
    public BasketResponse changeQuantity(String userId, Long bookId, int quantity) {
        Basket basket = findBasketByUserId(userId);
        BookItem item = items.findByBasketIdAndBookId(basket.getId(), bookId).orElseThrow(() -> new IllegalArgumentException("Book is not in basket"));
        books.requireAvailable(bookId, quantity);
        item.setQuantity(quantity);
        return toBasketResponse(basket);
    }

    private Basket findBasketByUserId(String userId) {
        return baskets.findByUserId(userId)
                .orElseGet(() -> baskets.save(Basket.builder().userId(userId).build()));
    }

    private BasketResponse toBasketResponse(Basket basket) {
        List<BasketItemResponse> result = items.findAllByBasketId(basket.getId()).stream()
                .map(mapper::toResponse)
                .toList();
        return new BasketResponse(basket.getId(), result);
    }
}
