package com.sxxm.stockknock.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}

