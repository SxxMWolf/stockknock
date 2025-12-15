package com.sxxm.stockknock.portfolio.entity;

import com.sxxm.stockknock.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne
    @JoinColumn(name = "stock_symbol", nullable = false, referencedColumnName = "symbol")
    private Stock stock;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity; // 보유 수량

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal avgBuyPrice; // 평균 매입 단가

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

