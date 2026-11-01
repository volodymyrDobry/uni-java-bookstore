package com.cengage.contenttaggingservice.unibookstore.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_item", uniqueConstraints = @UniqueConstraint(name = "uk_order_item_book", columnNames = {"ordering_id", "book_id"}))
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ordering_id", nullable = false, updatable = false)
    private Ordering ordering;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, updatable = false)
    private Book book;

    @Column(name = "count", nullable = false)
    private Integer count;

    // Price captured when book-management-service confirms the stock reservation.
    // Null while the order is still PENDING (awaiting the reservation reply); it
    // lets receipts be rebuilt locally without order-service reading stock data.
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
}
