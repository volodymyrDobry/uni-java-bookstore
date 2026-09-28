package com.cengage.contenttaggingservice.unibookstore.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Validated
@RequestMapping
public @interface ValidatedApi {

    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String[] path() default {};
}
