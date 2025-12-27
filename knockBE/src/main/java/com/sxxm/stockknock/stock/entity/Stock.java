/**
 * 주식 기본 정보 저장. stocks 테이블과 매핑, symbol을 Primary Key로 사용.
 */
package com.sxxm.stockknock.stock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {
    @Id
    @Column(length = 20, nullable = false)
    private String symbol; // 종목 코드 (예: AAPL, 005930) - PK

    @Column(nullable = false)
    private String name; // 종목명

    @Column(length = 50)
    private String exchange; // 거래소 (NYSE, NASDAQ, KOSPI, KOSDAQ)

    @Column(length = 50)
    private String country; // 국가 (US, KR)

    @Column(length = 100)
    private String industry; // 산업군

    @Column(length = 10)
    private String currency; // 통화 (USD, KRW)

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

