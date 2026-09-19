package com.cengage.contenttaggingservice.unibookstore.validator;

import com.cengage.contenttaggingservice.unibookstore.annotation.BookExists;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookExistsValidator implements ConstraintValidator<BookExists, Long> {

    private final BookRepository repository;

    @Override
    public boolean isValid(Long id, ConstraintValidatorContext context) {
        if (id == null) {
            return true;
        }

        return repository.existsById(id);
    }
}
