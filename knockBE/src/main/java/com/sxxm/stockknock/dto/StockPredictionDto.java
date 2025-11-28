package com.sxxm.stockknock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPredictionDto {
    private Long id;
    private String stockSymbol;
    private String predictionType;
    private BigDecimal predictedPrice;
    private LocalDate targetDate;
    private String analysis;
    private BigDecimal confidence;
}

