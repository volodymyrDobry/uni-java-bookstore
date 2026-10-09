package com.cengage.contenttaggingservice.unibookstore.aspect;

import com.cengage.contenttaggingservice.unibookstore.annotation.BookOwner;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.exception.NotFoundException;
import com.cengage.contenttaggingservice.unibookstore.exception.UserNotAllowedException;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BookOwnerAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final BookRepository bookRepository;
    private final SecurityUtils securityUtils;

    @Before("@annotation(bookOwner)")
    public void ownBook(JoinPoint joinPoint, BookOwner bookOwner) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, args, paramNameDiscoverer);

        Long bookId = (Long) parser
                .parseExpression(bookOwner.value())
                .getValue(context);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        String currentUseId = securityUtils.getCurrentUser().username();

        boolean isCreator = Objects.equals(book.getCreatorId(), currentUseId);

        if (!isCreator) {
            log.warn("Book: {} does not belong to user: {}", book, currentUseId);
            throw new UserNotAllowedException("Current user is not allowed to perform this action");
        }
    }


}
