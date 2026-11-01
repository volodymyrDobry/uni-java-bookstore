package com.cengage.contenttaggingservice.unibookstore.service.impl;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReserveStockCommand;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReserveStockLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.ReservedLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.ShortfallLine;
import com.cengage.contenttaggingservice.unibookstore.messaging.StockReservationResult;
import com.cengage.contenttaggingservice.unibookstore.repository.BookStockRepository;
import com.cengage.contenttaggingservice.unibookstore.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationServiceImpl implements StockReservationService {

    private final BookStockRepository bookStockRepository;

    @Override
    @Transactional
    public StockReservationResult reserve(ReserveStockCommand command) {
        // Check every line first so nothing is removed unless the whole order fits.
        List<ShortfallLine> shortfalls = new ArrayList<>();
        List<BookStock> stocks = new ArrayList<>(command.lines().size());
        for (ReserveStockLine line : command.lines()) {
            BookStock stock = bookStockRepository.findByBookId(line.bookId());
            int available = stock != null ? stock.getQuantity() : 0;
            if (stock == null || available < line.quantity()) {
                shortfalls.add(new ShortfallLine(line.bookId(), line.quantity(), available));
            }
            stocks.add(stock);
        }

        if (!shortfalls.isEmpty()) {
            log.warn("Rejecting order {}: insufficient stock for {} line(s)", command.orderId(), shortfalls.size());
            return StockReservationResult.insufficientStock(
                    command.orderId(), command.customerId(), command.createdAt(), shortfalls);
        }

        List<ReservedLine> reservedLines = new ArrayList<>(command.lines().size());
        for (int i = 0; i < command.lines().size(); i++) {
            ReserveStockLine line = command.lines().get(i);
            BookStock stock = stocks.get(i);
            stock.setQuantity(stock.getQuantity() - line.quantity());
            Book book = stock.getBook();
            reservedLines.add(new ReservedLine(
                    book.getId(), book.getTitle(), book.getAuthor(), line.quantity(), stock.getPrice()));
        }
        bookStockRepository.saveAll(stocks);

        log.info("Reserved and removed stock for order {} ({} line(s))", command.orderId(), reservedLines.size());
        return StockReservationResult.reserved(
                command.orderId(), command.customerId(), command.createdAt(), reservedLines);
    }
}
