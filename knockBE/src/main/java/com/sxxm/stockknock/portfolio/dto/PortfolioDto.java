package com.sxxm.stockknock.portfolio.dto;

/**
 * 포트폴리오 DTO
 * 
 * 역할:
 * - 포트폴리오 항목 정보를 프론트엔드로 전달
 * - 종목 정보, 보유 수량, 평균가, 현재가, 손익 정보 포함
 */
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

