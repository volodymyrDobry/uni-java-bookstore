package com.cengage.contenttaggingservice.unibookstore.controller.docs;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.QuantityRequest;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/basket")
@Tag(name = "Basket", description = "Authenticated user's basket")
public interface BasketController {
    @GetMapping
    @Operation(summary = "Get current basket")
    BasketResponse get(@AuthenticationPrincipal CurrentUser user);

    @PostMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add book quantity to basket")
    BasketResponse add(@AuthenticationPrincipal CurrentUser user, @PathVariable Long bookId, @Valid @RequestBody QuantityRequest request);

    @PatchMapping("/items/{bookId}")
    @Operation(summary = "Set book quantity in basket")
    BasketResponse quantity(@AuthenticationPrincipal CurrentUser user, @PathVariable Long bookId, @Valid @RequestBody QuantityRequest request);

    @DeleteMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove book from basket")
    void remove(@AuthenticationPrincipal CurrentUser user, @PathVariable Long bookId);
}
