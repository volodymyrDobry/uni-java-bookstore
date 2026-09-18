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
import com.cengage.contenttaggingservice.unibookstore.service.BasketService;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasketServiceImpl implements BasketService {

    private final BasketRepository basketRepository;
    private final SecurityUtils securityUtils;
    private final BasketMapper basketMapper;
    private final BookRepository bookRepository;

    @Override
    public BasketResponse getUsersBasket() {
        return basketMapper.toBasketResponse(this.getOrCreateNewBasket());
    }

    @Override
    @Transactional
    public BasketResponse addBookToTheBasket(UpdateBasketItemRequest request) {
        Basket userBasket = this.getOrCreateNewBasket();
        Book book = bookRepository.getReferenceById(request.bookId());
        BookItem updatedBookItem = userBasket.getBooks().stream()
                .filter(b -> Objects.equals(b.getId(), request.bookId()))
                .findFirst()
                .map(b -> b.addQuantity(request.quantity()))
                .orElse(new BookItem(null, request.quantity(), book, userBasket));
        userBasket.getBooks().add(updatedBookItem);

        return basketMapper.toBasketResponse(basketRepository.save(userBasket));
    }

    @Override
    @Transactional
    public BasketResponse updateBasketItems(List<UpdateBasketItemRequest> request) {
        Basket userBasket = this.getOrCreateNewBasket();
        Map<Long, Integer> bookQuantityMap = request.stream()
                .collect(Collectors.toMap(UpdateBasketItemRequest::bookId, UpdateBasketItemRequest::quantity));

        userBasket.getBooks().forEach(book -> {
            Integer quantity = bookQuantityMap.get(book.getId());
            if (quantity != null) {
                book.setQuantity(quantity);
            }
        });

        return basketMapper.toBasketResponse(basketRepository.save(userBasket));
    }

    @Override
    @Transactional
    public void removeBookFromBasket(Long bookId) {
        Basket userBasket = this.getOrCreateNewBasket();
        userBasket.getBooks().removeIf(b -> Objects.equals(b.getId(), bookId));
        basketRepository.save(userBasket);
    }

    private Basket getOrCreateNewBasket() {
        CurrentUser currentUser = securityUtils.getCurrentUser();
        return basketRepository.findByUserId(currentUser.username())
                .orElse(basketRepository.save(new Basket(null, currentUser.username(), Set.of())));
    }
}
