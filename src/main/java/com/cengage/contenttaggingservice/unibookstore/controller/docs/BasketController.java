package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Validated
@RequestMapping("/api/v1/basket")
@Tag(name = "Basket", description = "Authenticated user's basket")
public interface BasketController {

    @GetMapping
    @Operation(summary = "Get currently authenticated user basket")
    BasketResponse getUsersBasket();

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add book quantity to the existing basket or creates a new one")
    BasketResponse addBookToTheBasket(@Valid @RequestBody UpdateBasketItemRequest request);

    @PatchMapping("/items")
    @Operation(summary = "Set book quantity in basket")
    BasketResponse updateBookItemsQuantity(@Valid @RequestBody List<UpdateBasketItemRequest> request);

    @DeleteMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove book from basket")
    void remove(@PathVariable Long bookId);
}
