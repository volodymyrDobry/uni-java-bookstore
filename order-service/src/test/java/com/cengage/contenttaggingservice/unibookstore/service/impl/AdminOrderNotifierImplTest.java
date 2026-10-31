package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AdminOrderNotifierImplTest {

    private final AdminOrderNotifierImpl notifier = new AdminOrderNotifierImpl();

    private OrderSnapshot snapshot() {
        return new OrderSnapshot(1L, "alice", Instant.now(), List.of(), new BigDecimal("12.50"));
    }

    @Test
    void subscribeReturnsEmitter() {
        SseEmitter emitter = notifier.subscribe();
        assertThat(emitter).isNotNull();
    }

    @Test
    void notifyWithNoSubscribersIsANoOp() {
        assertThatCode(() -> notifier.notifyNewOrder(snapshot())).doesNotThrowAnyException();
    }

    @Test
    void notifyDeliversToActiveSubscriberWithoutThrowing() {
        notifier.subscribe();
        assertThatCode(() -> notifier.notifyNewOrder(snapshot())).doesNotThrowAnyException();
    }

    @Test
    void erroredSubscriberIsDropped() {
        SseEmitter emitter = notifier.subscribe();
        emitter.completeWithError(new RuntimeException("client gone"));
        assertThatCode(() -> notifier.notifyNewOrder(snapshot())).doesNotThrowAnyException();
    }
}
