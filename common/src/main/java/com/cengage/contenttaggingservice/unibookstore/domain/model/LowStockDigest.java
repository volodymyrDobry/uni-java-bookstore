package com.cengage.contenttaggingservice.unibookstore.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A stored low-stock report produced by the scheduled catalogue job, kept so the
 * admin UI can show the history of digests and which books were running low.
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "low_stock_digest")
public class LowStockDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "threshold", nullable = false)
    private int threshold;

    @Column(name = "only_enabled", nullable = false)
    private boolean onlyEnabled;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    @Builder.Default
    @OneToMany(mappedBy = "digest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LowStockDigestItem> items = new ArrayList<>();

    public void addItem(LowStockDigestItem item) {
        item.setDigest(this);
        this.items.add(item);
    }
}
