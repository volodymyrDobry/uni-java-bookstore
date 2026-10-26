package com.cengage.contenttaggingservice.unibookstore.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cengage.contenttaggingservice.unibookstore.domain.enums.Genre;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.CreateBookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import com.cengage.contenttaggingservice.unibookstore.mapper.BookMapper;
import com.cengage.contenttaggingservice.unibookstore.repository.BookRepository;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.service.CRUDBookService;
import com.cengage.contenttaggingservice.unibookstore.service.impl.CRUDBookServiceImpl;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CRUDBookServiceAuditAspectTest {

    private static final Long BOOK_ID = 42L;

    private AnnotationConfigApplicationContext context;
    private CRUDBookService service;
    private BookRepository repository;
    private BookMapper mapper;
    private SecurityUtils securityUtils;
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        repository = mock(BookRepository.class);
        mapper = mock(BookMapper.class);
        securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUser()).thenReturn(new CurrentUser("alice", List.of("USER")));

        context = new AnnotationConfigApplicationContext();
        context.registerBean(BookRepository.class, () -> repository);
        context.registerBean(BookMapper.class, () -> mapper);
        context.registerBean(SecurityUtils.class, () -> securityUtils);
        context.registerBean(AuditAspect.class);
        context.registerBean(CRUDBookServiceImpl.class);
        context.register(AopConfig.class);
        context.refresh();

        service = context.getBean(CRUDBookService.class);

        auditLogger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
        appender = new ListAppender<>();
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAppender(appender);
        context.close();
    }

    @Test
    void createBookThroughProxyIsAudited() {
        stubSuccessfulSave();

        service.createBook(request());

        assertThat(auditEvents("BOOK_CREATED")).hasSize(1);
        assertThat(auditEvents("BOOK_CREATED").get(0).getFormattedMessage()).contains("status=SUCCESS");
    }

    @Test
    void bulkCreateDoesNotAuditInnerSelfInvokedCreateBook() {
        stubSuccessfulSave();

        service.createBooks(List.of(request(), request()));

        assertThat(auditEvents("BOOKS_BULK_CREATED")).hasSize(1);
        assertThat(auditEvents("BOOK_CREATED")).isEmpty();
        verify(repository, times(2)).save(any(Book.class));
    }

    @Test
    void updateBookDetailsFailureIsAuditedAndExceptionPropagates() {
        when(repository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBookDetails(BOOK_ID, updateRequest()))
                .isInstanceOf(NoSuchElementException.class);

        List<ILoggingEvent> events = auditEvents("BOOK_UPDATED");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(events.get(0).getFormattedMessage()).contains("status=FAILURE");
    }

    private void stubSuccessfulSave() {
        when(repository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Book.class))).thenReturn(response());
    }

    private List<ILoggingEvent> auditEvents(String action) {
        return appender.list.stream()
                .filter(event -> event.getFormattedMessage().contains("action=" + action + " "))
                .toList();
    }

    private CreateBookRequest request() {
        return new CreateBookRequest("Dune", "desc", "Herbert", Genre.SCIENCE_FICTION, "img",
                true, new BigDecimal("9.99"), 10);
    }

    private UpdateBookDetailsRequest updateRequest() {
        return new UpdateBookDetailsRequest("New", "desc", "Herbert", Genre.FANTASY, "img", true);
    }

    private BookResponse response() {
        return new BookResponse(BOOK_ID, "Dune", "desc", "Herbert",
                Genre.SCIENCE_FICTION, "img", true, 10, new BigDecimal("9.99"));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AopConfig {
    }
}
