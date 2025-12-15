package com.sxxm.stockknock.portfolio.dto;

import com.sxxm.stockknock.stock.dto.StockDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDto {
    private Long id; // PortfolioItem id
    private Long portfolioId;
    private String portfolioName;
    private StockDto stock;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossRate;
}

