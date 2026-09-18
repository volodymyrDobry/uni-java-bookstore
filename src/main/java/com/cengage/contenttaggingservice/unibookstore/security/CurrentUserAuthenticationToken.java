package com.cengage.contenttaggingservice.unibookstore.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class CurrentUserAuthenticationToken extends JwtAuthenticationToken {

    private final CurrentUser currentUser;

    public CurrentUserAuthenticationToken(Jwt jwt, CurrentUser currentUser,
                                          Collection<GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.currentUser = currentUser;
    }

    @Override
    public Object getPrincipal() {
        return currentUser;
    }
}
