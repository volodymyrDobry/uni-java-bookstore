package com.cengage.contenttaggingservice.unibookstore.annotation;

import com.cengage.contenttaggingservice.unibookstore.controller.BookStockControllerImpl;
import com.cengage.contenttaggingservice.unibookstore.dto.BookStockResponse;
import com.cengage.contenttaggingservice.unibookstore.service.BookStockService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValidatedApiTest {

    @Test
    void composesValidatedAndRequestMapping() {
        assertThat(ValidatedApi.class.isAnnotationPresent(Validated.class)).isTrue();
        assertThat(ValidatedApi.class.isAnnotationPresent(RequestMapping.class)).isTrue();
    }

    @Test
    void bookStockEndpointUsesComposedPathMapping() throws Exception {
        BookStockService service = mock(BookStockService.class);
        when(service.updateBookStock(42L, new com.cengage.contenttaggingservice.unibookstore.dto.StockRequest(
                5, new BigDecimal("9.99"))))
                .thenReturn(new BookStockResponse(42L, new BigDecimal("9.99"), 5));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BookStockControllerImpl(service)).build();

        mockMvc.perform(put("/api/v1/books/42/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5,\"price\":9.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(42))
                .andExpect(jsonPath("$.quantity").value(5));

        verify(service).updateBookStock(42L,
                new com.cengage.contenttaggingservice.unibookstore.dto.StockRequest(5, new BigDecimal("9.99")));
    }
}
