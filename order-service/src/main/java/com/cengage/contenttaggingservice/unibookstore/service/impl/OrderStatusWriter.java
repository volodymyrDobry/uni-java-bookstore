package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.enums.OrderStatus;
import com.cengage.contenttaggingservice.unibookstore.repository.OrderingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusWriter {

    private final OrderingRepository orderingRepository;

    @Transactional
    public void markReady(Long orderId, String pdfLocation) {
        orderingRepository.findById(orderId).ifPresent(order -> {
            order.setPdfLocation(pdfLocation);
            order.setStatus(OrderStatus.PDF_READY);
        });
    }

    @Transactional
    public void markFailed(Long orderId) {
        orderingRepository.findById(orderId).ifPresent(order -> order.setStatus(OrderStatus.FAILED));
    }
}
