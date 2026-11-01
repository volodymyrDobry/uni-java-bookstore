package com.cengage.contenttaggingservice.unibookstore.messaging;

import com.cengage.contenttaggingservice.unibookstore.service.OrderReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockReservationReplyListenerTest {

    @Mock
    private OrderReservationService orderReservationService;

    @InjectMocks
    private StockReservationReplyListener listener;

    @Test
    void onResultDelegatesToTheService() {
        StockReservationResult result = StockReservationResult.reserved(5L, "alice", Instant.now(), List.of());

        listener.onResult(result);

        verify(orderReservationService).applyResult(result);
    }
}
