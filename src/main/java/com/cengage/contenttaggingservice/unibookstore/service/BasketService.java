package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;

import java.util.List;

public interface BasketService {
    BasketResponse getUsersBasket(String username);

    BasketResponse addBookToTheBasket(UpdateBasketItemRequest request);

    BasketResponse updateBasketItems(List<UpdateBasketItemRequest> request);

    void removeBookFromBasket(Long bookId);
}
