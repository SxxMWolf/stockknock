package com.sxxm.stockknock.dto;

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
    private Long id;
    private String symbol;
    private String name;
    private String exchange;
    private String country;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    private BigDecimal marketCap;
    private BigDecimal peRatio;
    private BigDecimal dividendYield;
}

