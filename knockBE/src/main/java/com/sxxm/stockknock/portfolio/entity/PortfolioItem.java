/**
 * 포트폴리오에 포함된 종목 정보 저장. 보유 수량, 평균 매입가 저장.
 */
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_symbol", nullable = false, referencedColumnName = "symbol")
    private Stock stock;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity; // 보유 수량

    @Column(name = "avg_buy_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal avgBuyPrice; // 평균 매입 단가

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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

