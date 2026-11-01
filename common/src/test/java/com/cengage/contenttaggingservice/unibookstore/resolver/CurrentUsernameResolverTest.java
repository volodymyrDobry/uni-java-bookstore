package com.cengage.contenttaggingservice.unibookstore.resolver;

import com.cengage.contenttaggingservice.unibookstore.annotation.CurrentUsername;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUserAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUsernameResolverTest {

    private final CurrentUsernameResolver resolver = new CurrentUsernameResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsOnlyStringParametersAnnotatedWithCurrentUsername() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameter("annotatedUsername", String.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("plainUsername", String.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("annotatedNonString", Long.class))).isFalse();
    }

    @Test
    void resolveArgumentReturnsUsernameFromAuthenticatedPrincipal() throws Exception {
        authenticateAs("book-reader");

        Object resolved = resolver.resolveArgument(
                parameter("annotatedUsername", String.class),
                null,
                new ServletWebRequest(new MockHttpServletRequest()),
                null);

        assertThat(resolved).isEqualTo("book-reader");
    }

    @Test
    void resolveArgumentRejectsMissingCurrentUserAuthentication() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(
                parameter("annotatedUsername", String.class),
                null,
                new ServletWebRequest(new MockHttpServletRequest()),
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Current username resolver requires authentication");
    }

    private MethodParameter parameter(String methodName, Class<?> parameterType) throws NoSuchMethodException {
        Method method = TestEndpoint.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
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

    @SuppressWarnings("unused")
    private static class TestEndpoint {
        void annotatedUsername(@CurrentUsername String username) {
        }

        void plainUsername(String username) {
        }

        void annotatedNonString(@CurrentUsername Long userId) {
        }
    }
}
