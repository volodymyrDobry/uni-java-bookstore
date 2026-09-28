package com.cengage.contenttaggingservice.unibookstore.annotation;

import com.cengage.contenttaggingservice.unibookstore.validator.BookExistsValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Constraint(validatedBy = {BookExistsValidator.class})
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface BookExists {
    String message() default "Book with the given id does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
