package com.cengage.contenttaggingservice.unibookstore.validator;

import com.cengage.contenttaggingservice.unibookstore.annotation.BookExists;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBasketItemRequest;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateBasketItemBookExistsValidator implements ConstraintValidator<BookExists, UpdateBasketItemRequest> {

    private final BookRepository repository;

    @Override
    public boolean isValid(UpdateBasketItemRequest request, ConstraintValidatorContext context) {
        return request == null || request.bookId() == null || repository.existsById(request.bookId());
    }
}
