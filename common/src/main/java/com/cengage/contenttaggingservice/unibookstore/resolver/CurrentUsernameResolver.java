package com.cengage.contenttaggingservice.unibookstore.resolver;

import com.cengage.contenttaggingservice.unibookstore.annotation.CurrentUsername;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUserAuthenticationToken;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUsernameResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        return parameter.hasParameterAnnotation(CurrentUsername.class)
                && parameter.getParameterType()
                .equals(String.class);
    }

    @Override
    public @Nullable Object resolveArgument(@NonNull MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            @NonNull NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof CurrentUserAuthenticationToken token) {
            CurrentUser user = (CurrentUser) token.getPrincipal();
            return user.username();
        }

        throw new IllegalArgumentException("Current username resolver requires authentication");
    }

}
