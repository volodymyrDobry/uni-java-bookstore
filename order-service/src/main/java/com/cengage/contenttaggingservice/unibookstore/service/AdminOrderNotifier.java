package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AdminOrderNotifier {

    SseEmitter subscribe();

    void notifyNewOrder(OrderSnapshot snapshot);
}
