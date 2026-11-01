package com.cengage.contenttaggingservice.unibookstore.messaging;

import com.cengage.contenttaggingservice.unibookstore.service.StockReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationListenerTest {

    @Mock
    private StockReservationService stockReservationService;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @InjectMocks
    private StockReservationListener listener;

    private ReserveStockCommand command() {
        return new ReserveStockCommand(5L, "alice", Instant.now(), List.of(new ReserveStockLine(1L, 2)));
    }

    @Test
    void reservesThenPublishesTheResult() {
        ReserveStockCommand command = command();
        StockReservationResult result = StockReservationResult.reserved(5L, "alice", command.createdAt(), List.of());
        when(stockReservationService.reserve(command)).thenReturn(result);

        listener.onReserveRequest(command);

        verify(stockReservationService).reserve(command);
        verify(stockEventPublisher).publishResult(result);
    }

    @Test
    void publishesAnErrorResultWhenReservationThrows() {
        ReserveStockCommand command = command();
        when(stockReservationService.reserve(command)).thenThrow(new RuntimeException("db down"));

        listener.onReserveRequest(command);

        ArgumentCaptor<StockReservationResult> captor = ArgumentCaptor.forClass(StockReservationResult.class);
        verify(stockEventPublisher).publishResult(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReservationStatus.ERROR);
        assertThat(captor.getValue().orderId()).isEqualTo(5L);
        assertThat(captor.getValue().message()).isEqualTo("db down");
    }
}
