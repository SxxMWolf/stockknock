package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String symbol; // 종목 코드 (예: AAPL, 005930)

    @Column(nullable = false)
    private String name; // 종목명

    private String exchange; // 거래소 (NYSE, NASDAQ, KOSPI, KOSDAQ)

    private String country; // 국가 (US, KR)

    @ManyToOne
    @JoinColumn(name = "industry_id")
    private Industry industry;

    private BigDecimal currentPrice;

    private BigDecimal previousClose;

    private BigDecimal dayHigh;

    private BigDecimal dayLow;

    private Long volume;

    private BigDecimal marketCap;

    private BigDecimal peRatio;

    private BigDecimal dividendYield;

    private LocalDateTime lastUpdated;

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}

