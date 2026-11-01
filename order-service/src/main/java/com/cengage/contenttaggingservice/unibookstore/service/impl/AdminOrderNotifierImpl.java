package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.config.AsyncConfig;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderNotification;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import com.cengage.contenttaggingservice.unibookstore.service.AdminOrderNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class AdminOrderNotifierImpl implements AdminOrderNotifier {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Override
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        emitters.add(emitter);
        log.info("Admin subscribed to order stream; active subscribers={}", emitters.size());
        return emitter;
    }

    @Override
    @Async(AsyncConfig.ORDERING_EXECUTOR)
    public void notifyNewOrder(OrderSnapshot snapshot) {
        OrderNotification payload = new OrderNotification(
                snapshot.orderId(), snapshot.customerId(), snapshot.lines().size(), snapshot.grandTotal());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("order").data(payload));
            } catch (IOException | RuntimeException ex) {
                log.warn("Dropping admin subscriber after failed send: {}", ex.getMessage());
                emitters.remove(emitter);
            }
        }
        log.info("Notified admins about order {} (subscribers={})", snapshot.orderId(), emitters.size());
    }
}
