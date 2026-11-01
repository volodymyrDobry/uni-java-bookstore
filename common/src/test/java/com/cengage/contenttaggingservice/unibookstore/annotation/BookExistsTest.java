package com.cengage.contenttaggingservice.unibookstore.annotation;

import com.cengage.contenttaggingservice.unibookstore.validator.BookExistsValidator;
import com.cengage.contenttaggingservice.unibookstore.validator.UpdateBasketItemBookExistsValidator;
import jakarta.validation.Constraint;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

class BookExistsTest {

    @Test
    void hasDefaultMessage() throws Exception {
        String message = (String) BookExists.class.getMethod("message").getDefaultValue();
        assertThat(message).isEqualTo("Book with the given id does not exist");
    }

    @Test
    void isWiredToValidatorsForBookIdAndBasketItemRequest() {
        Constraint constraint = BookExists.class.getAnnotation(Constraint.class);
        assertThat(constraint.validatedBy()).containsExactlyInAnyOrder(
                BookExistsValidator.class,
                UpdateBasketItemBookExistsValidator.class);
    }

    @Test
    void targetsDtosFieldsAndParameters() {
        Target target = BookExists.class.getAnnotation(Target.class);
        assertThat(target.value())
                .containsExactlyInAnyOrder(ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER);
    }

    @Test
    void isRetainedAtRuntime() {
        Retention retention = BookExists.class.getAnnotation(Retention.class);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }
}
