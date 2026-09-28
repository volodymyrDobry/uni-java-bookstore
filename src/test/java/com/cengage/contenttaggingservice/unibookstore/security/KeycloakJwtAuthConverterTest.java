package com.cengage.contenttaggingservice.unibookstore.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthConverterTest {

    private final KeycloakJwtAuthConverter converter = new KeycloakJwtAuthConverter();

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "Vova")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void convertMapsRolesToPrefixedAuthoritiesAndBuildsPrincipal() {
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "USER")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isInstanceOf(CurrentUserAuthenticationToken.class);
        assertThat(token.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");

        CurrentUser principal = (CurrentUser) token.getPrincipal();
        assertThat(principal.username()).isEqualTo("Vova");
        assertThat(principal.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void convertHandlesMissingRealmAccess() {
        Jwt jwt = baseJwt().build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
        assertThat(((CurrentUser) token.getPrincipal()).roles()).isEmpty();
    }

    @Test
    void convertHandlesRealmAccessWithoutRoles() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("other", "x")).build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }
}
