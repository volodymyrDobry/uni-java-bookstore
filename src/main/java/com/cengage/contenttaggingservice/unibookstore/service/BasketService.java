package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;

public interface BasketService {
    BasketResponse add(String userId, Long bookId, int quantity);

    BasketResponse get(String userId);

    void remove(String userId, Long bookId);

    BasketResponse changeQuantity(String userId, Long bookId, int quantity);
}
