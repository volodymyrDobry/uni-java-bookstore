package com.cengage.contenttaggingservice.unibookstore.controller;

import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import com.cengage.contenttaggingservice.unibookstore.service.BasketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketControllerImplTest {

    @Mock
    private BasketService basketService;

    @InjectMocks
    private BasketControllerImpl controller;

    @Test
    void getUsersBasketDelegatesToService() {
        BasketResponse expected = new BasketResponse(1L, List.of());
        when(basketService.getUsersBasket("vovko")).thenReturn(expected);

        assertThat(controller.getUsersBasket("vovko")).isSameAs(expected);
        verify(basketService).getUsersBasket("vovko");
    }

    @Test
    void addBookToTheBasketDelegatesToService() {
        UpdateBasketItemRequest request = new UpdateBasketItemRequest(42L, 2);
        BasketResponse expected = new BasketResponse(1L, List.of());
        when(basketService.addBookToTheBasket(request)).thenReturn(expected);

        assertThat(controller.addBookToTheBasket(request)).isSameAs(expected);
    }

    @Test
    void updateBookItemsQuantityDelegatesToService() {
        List<UpdateBasketItemRequest> request = List.of(new UpdateBasketItemRequest(42L, 2));
        BasketResponse expected = new BasketResponse(1L, List.of());
        when(basketService.updateBasketItems(request)).thenReturn(expected);

        assertThat(controller.updateBookItemsQuantity(request)).isSameAs(expected);
    }

    @Test
    void removeDelegatesToService() {
        controller.remove(42L);

        verify(basketService).removeBookFromBasket(42L);
        verifyNoMoreInteractions(basketService);
    }
}
