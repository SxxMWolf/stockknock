package com.sxxm.stockknock.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_price_history", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"stock_symbol", "timestamp"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_symbol", nullable = false, referencedColumnName = "symbol")
    private Stock stock;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price; // 종가/현재가

    @Column(precision = 18, scale = 4)
    private BigDecimal open;

    @Column(precision = 18, scale = 4)
    private BigDecimal high;

    @Column(precision = 18, scale = 4)
    private BigDecimal low;

    private Long volume;

    @Column(nullable = false)
    private LocalDateTime timestamp; // 수집 시간

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

