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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One low-stock book captured in a {@link LowStockDigest}. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "low_stock_digest_item")
public class LowStockDigestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "digest_id", nullable = false, updatable = false)
    private LowStockDigest digest;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
