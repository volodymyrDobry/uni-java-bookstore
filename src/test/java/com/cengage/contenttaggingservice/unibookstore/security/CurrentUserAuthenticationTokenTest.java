package com.cengage.contenttaggingservice.unibookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserAuthenticationTokenTest {

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "Vova")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void getPrincipalReturnsCurrentUser() {
        CurrentUser user = new CurrentUser("Vova", List.of("ADMIN"));
        CurrentUserAuthenticationToken token = new CurrentUserAuthenticationToken(
                jwt(), user, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(token.getPrincipal()).isSameAs(user);
        assertThat(token.getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
    }
}
