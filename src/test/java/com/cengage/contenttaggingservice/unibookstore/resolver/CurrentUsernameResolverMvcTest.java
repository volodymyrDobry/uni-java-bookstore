package com.cengage.contenttaggingservice.unibookstore.resolver;

import com.cengage.contenttaggingservice.unibookstore.controller.BasketControllerImpl;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUserAuthenticationToken;
import com.cengage.contenttaggingservice.unibookstore.service.BasketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUsernameResolverMvcTest {

    private BasketService basketService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        basketService = mock(BasketService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new BasketControllerImpl(basketService))
                .setCustomArgumentResolvers(new CurrentUsernameResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void basketEndpointPassesResolvedUsernameToControllerLogic() throws Exception {
        authenticateAs("book-reader");
        when(basketService.getUsersBasket("book-reader"))
                .thenReturn(new BasketResponse(1L, List.of()));

        mockMvc.perform(get("/api/v1/basket"))
                .andExpect(status().isOk());

        verify(basketService).getUsersBasket("book-reader");
    }

    private void authenticateAs(String username) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new CurrentUserAuthenticationToken(
                jwt, new CurrentUser(username, List.of("USER")), List.of()));
    }
}
