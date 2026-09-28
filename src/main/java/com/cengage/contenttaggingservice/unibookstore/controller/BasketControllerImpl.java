package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.controller.docs.BasketController;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import com.cengage.contenttaggingservice.unibookstore.service.BasketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BasketControllerImpl implements BasketController {

    private final BasketService basketService;

    @Override
    public BasketResponse getUsersBasket() {
        return basketService.getUsersBasket();
    }

    @Override
    public BasketResponse addBookToTheBasket(UpdateBasketItemRequest request) {
        return basketService.addBookToTheBasket(request);
    }

    @Override
    public BasketResponse updateBookItemsQuantity(List<UpdateBasketItemRequest> request) {
        return basketService.updateBasketItems(request);
    }

    @Override
    public void remove(Long bookId) {
        basketService.removeBookFromBasket(bookId);
    }
}
