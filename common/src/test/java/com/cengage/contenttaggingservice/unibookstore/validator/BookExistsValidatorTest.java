package com.cengage.contenttaggingservice.unibookstore.validator;

import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookExistsValidatorTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookExistsValidator validator;

    @Test
    void nullIdIsValidAndRepositoryNotQueried() {
        assertThat(validator.isValid(null, null)).isTrue();
        verifyNoInteractions(repository);
    }

    @Test
    void existingBookIsValid() {
        when(repository.existsById(42L)).thenReturn(true);
        assertThat(validator.isValid(42L, null)).isTrue();
    }

    @Test
    void missingBookIsInvalid() {
        when(repository.existsById(42L)).thenReturn(false);
        assertThat(validator.isValid(42L, null)).isFalse();
    }
}
