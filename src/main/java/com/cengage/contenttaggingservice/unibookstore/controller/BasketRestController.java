package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.controller.docs.BasketController;
import com.cengage.contenttaggingservice.unibookstore.dto.QuantityRequest;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.service.BasketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BasketRestController implements BasketController {

    private final BasketService service;

    @Override
    public BasketResponse get(CurrentUser user) {
        return service.get(user.username());
    }

    @Override
    public BasketResponse add(CurrentUser user, Long bookId, QuantityRequest request) {
        return service.add(user.username(), bookId, request.quantity());
    }

    @Override
    public BasketResponse quantity(CurrentUser user, Long bookId, QuantityRequest request) {
        return service.changeQuantity(user.username(), bookId, request.quantity());
    }

    @Override
    public void remove(CurrentUser user, Long bookId) {
        service.remove(user.username(), bookId);
    }
}
