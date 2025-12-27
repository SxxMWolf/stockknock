package com.sxxm.stockknock.stock.dto;

/**
 * 주식 DTO
 * 
 * 역할:
 * - 주식 정보를 프론트엔드로 전달
 * - 종목 기본 정보 (심볼, 이름, 시장) 및 실시간 가격 정보 포함
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDto {
    private String symbol;
    private String name;
    private String exchange;
    private String country;
    private String industry;
    private String currency;
    // 실시간 가격 정보는 StockPriceHistory에서 가져옴
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    // 가격 업데이트 시점 (참고용)
    private LocalDateTime lastUpdated;
}

