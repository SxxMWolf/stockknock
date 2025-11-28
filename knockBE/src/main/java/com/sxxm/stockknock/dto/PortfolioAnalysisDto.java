package com.sxxm.stockknock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisDto {
    private BigDecimal totalValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossRate;
    private String analysis; // AI 분석 결과
    private String investmentStyle;
}

