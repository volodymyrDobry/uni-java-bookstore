package com.cengage.contenttaggingservice.unibookstore.annotation;

import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.validator.BookExistsValidator;
import com.cengage.contenttaggingservice.unibookstore.validator.UpdateBasketItemBookExistsValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookExistsValidationTest {

    private AnnotationConfigApplicationContext applicationContext;
    private LocalValidatorFactoryBean validatorFactory;
    private BookRepository bookRepository;
    private Validator validator;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.registerBean(BookRepository.class, () -> bookRepository);
        applicationContext.register(BookExistsValidator.class, UpdateBasketItemBookExistsValidator.class);
        applicationContext.refresh();

        validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setConstraintValidatorFactory(
                new SpringConstraintValidatorFactory(applicationContext.getAutowireCapableBeanFactory()));
        validatorFactory.afterPropertiesSet();
        validator = validatorFactory;
    }

    @AfterEach
    void tearDown() {
        validatorFactory.destroy();
        applicationContext.close();
    }

    @Test
    void validBasketItemRequestHasNoViolations() {
        when(bookRepository.existsById(42L)).thenReturn(true);

        Set<ConstraintViolation<UpdateBasketItemRequest>> violations =
                validator.validate(new UpdateBasketItemRequest(42L, 1));

        assertThat(violations).isEmpty();
        verify(bookRepository).existsById(42L);
    }

    @Test
    void missingBookProducesClearConstraintMessage() {
        when(bookRepository.existsById(42L)).thenReturn(false);

        Set<ConstraintViolation<UpdateBasketItemRequest>> violations =
                validator.validate(new UpdateBasketItemRequest(42L, 1));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("Book with the given id does not exist");
    }

    @Test
    void standardNotNullConstraintStillAppliesToDto() {
        Set<ConstraintViolation<UpdateBasketItemRequest>> violations =
                validator.validate(new UpdateBasketItemRequest(null, 1));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("must not be null");
        verifyNoInteractions(bookRepository);
    }
}
