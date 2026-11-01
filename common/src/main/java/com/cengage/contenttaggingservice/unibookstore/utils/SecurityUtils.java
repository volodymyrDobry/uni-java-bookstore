package com.cengage.contenttaggingservice.unibookstore.utils;

import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUserAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private static final String ADMIN_ROLE = "ADMIN";

    public boolean isCurrentUserAdmin() {
        CurrentUser user = this.getCurrentUser();
        return user.hasRole(ADMIN_ROLE);
    }

    public CurrentUser getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof CurrentUserAuthenticationToken token) {
            return (CurrentUser) token.getPrincipal();
        }

        throw new IllegalArgumentException("There is no currently available user session");
    }

}
