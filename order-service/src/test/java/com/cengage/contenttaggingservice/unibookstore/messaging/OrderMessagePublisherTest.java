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
class OrderMessagePublisherTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @InjectMocks
    private OrderMessagePublisher publisher;

    @Test
    void requestStockReservationSendsCommandToReserveRequestQueue() {
        ReserveStockCommand command = new ReserveStockCommand(5L, "alice", Instant.now(),
                List.of(new ReserveStockLine(1L, 2)));

        publisher.requestStockReservation(command);

        verify(jmsTemplate).convertAndSend(JmsQueues.STOCK_RESERVE_REQUEST, command);
    }
}
