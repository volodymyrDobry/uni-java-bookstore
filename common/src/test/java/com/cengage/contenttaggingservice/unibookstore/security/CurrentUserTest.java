package com.cengage.contenttaggingservice.unibookstore.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void hasRoleReturnsTrueWhenPresent() {
        CurrentUser user = new CurrentUser("Vova", List.of("ADMIN", "USER"));
        assertThat(user.hasRole("ADMIN")).isTrue();
    }

    @Test
    void hasRoleReturnsFalseWhenAbsent() {
        CurrentUser user = new CurrentUser("Vova", List.of("USER"));
        assertThat(user.hasRole("ADMIN")).isFalse();
    }
}
