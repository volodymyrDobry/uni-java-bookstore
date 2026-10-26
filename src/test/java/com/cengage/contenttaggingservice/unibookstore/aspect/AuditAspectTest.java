package com.cengage.contenttaggingservice.unibookstore.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.cengage.contenttaggingservice.unibookstore.annotation.Auditable;
import com.cengage.contenttaggingservice.unibookstore.security.CurrentUser;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    private AnnotationConfigApplicationContext context;
    private SampleService sampleService;
    private SecurityUtils securityUtils;
    private Logger auditLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        securityUtils = mock(SecurityUtils.class);

        context = new AnnotationConfigApplicationContext();
        context.registerBean(SecurityUtils.class, () -> securityUtils);
        context.registerBean(AuditAspect.class);
        context.registerBean(SampleService.class);
        context.register(AopConfig.class);
        context.refresh();

        sampleService = context.getBean(SampleService.class);

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
    void successfulInvocationIsAuditedAtInfoWithUserAndAction() {
        when(securityUtils.getCurrentUser()).thenReturn(new CurrentUser("alice", List.of("USER")));

        String result = sampleService.doWork("payload");

        assertThat(result).isEqualTo("ok:payload");
        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .contains("action=SAMPLE_ACTION")
                .contains("status=SUCCESS")
                .contains("user=alice");
    }

    @Test
    void throwingInvocationIsAuditedAtWarnAndExceptionPropagates() {
        when(securityUtils.getCurrentUser()).thenReturn(new CurrentUser("alice", List.of("USER")));

        assertThatThrownBy(() -> sampleService.boom())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kaboom");

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage())
                .contains("action=SAMPLE_FAILURE")
                .contains("status=FAILURE")
                .contains("user=alice")
                .contains("IllegalStateException");
    }

    @Test
    void whenNoUserSessionThenAuditFallsBackToAnonymous() {
        when(securityUtils.getCurrentUser())
                .thenThrow(new IllegalArgumentException("There is no currently available user session"));

        sampleService.doWork("payload");

        assertThat(singleEvent().getFormattedMessage()).contains("user=anonymous");
    }

    private ILoggingEvent singleEvent() {
        assertThat(appender.list).hasSize(1);
        return appender.list.get(0);
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class AopConfig {
    }

    @Component
    static class SampleService {

        @Auditable("SAMPLE_ACTION")
        public String doWork(String input) {
            return "ok:" + input;
        }

        @Auditable("SAMPLE_FAILURE")
        public void boom() {
            throw new IllegalStateException("kaboom");
        }
    }
}
