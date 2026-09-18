package com.cengage.contenttaggingservice.unibookstore.security;

import java.util.List;

public record CurrentUser(String username, List<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
