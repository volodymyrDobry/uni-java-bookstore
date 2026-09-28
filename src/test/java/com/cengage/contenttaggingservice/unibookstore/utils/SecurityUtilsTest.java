package com.cengage.contenttaggingservice.unibookstore.utils;

import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUserAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(CurrentUser user) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .claim("sub", "x")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        CurrentUserAuthenticationToken token = new CurrentUserAuthenticationToken(
                jwt, user, List.of(new SimpleGrantedAuthority("ROLE_X")));
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void getCurrentUserReturnsPrincipal() {
        CurrentUser user = new CurrentUser("Vova", List.of("USER"));
        authenticateAs(user);

        assertThat(securityUtils.getCurrentUser()).isSameAs(user);
    }

    @Test
    void isCurrentUserAdminTrueWhenAdminRolePresent() {
        authenticateAs(new CurrentUser("Vova", List.of("ADMIN")));
        assertThat(securityUtils.isCurrentUserAdmin()).isTrue();
    }

    @Test
    void isCurrentUserAdminFalseWhenNoAdminRole() {
        authenticateAs(new CurrentUser("Vova", List.of("USER")));
        assertThat(securityUtils.isCurrentUserAdmin()).isFalse();
    }

    @Test
    void getCurrentUserThrowsWhenNoCurrentUserToken() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("x", "y"));

        assertThatThrownBy(securityUtils::getCurrentUser)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
