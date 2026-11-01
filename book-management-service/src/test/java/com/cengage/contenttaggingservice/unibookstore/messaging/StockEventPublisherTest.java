package com.cengage.contenttaggingservice.unibookstore.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockEventPublisherTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @InjectMocks
    private StockEventPublisher publisher;

    @Test
    void publishesTheResultToTheResultQueue() {
        StockReservationResult result = StockReservationResult.reserved(5L, "alice", Instant.now(), List.of());

        publisher.publishResult(result);

        verify(jmsTemplate).convertAndSend(JmsQueues.STOCK_RESERVE_RESULT, result);
    }
}
