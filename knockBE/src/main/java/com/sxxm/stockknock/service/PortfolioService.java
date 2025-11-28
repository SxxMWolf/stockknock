package com.sxxm.stockknock.service;

import com.sxxm.stockknock.ai.AIService;
import com.sxxm.stockknock.dto.PortfolioDto;
import com.sxxm.stockknock.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.entity.Portfolio;
import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.entity.User;
import com.sxxm.stockknock.repository.PortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StockService stockService;

    @Autowired
    private AIService aiService;

    public List<PortfolioDto> getUserPortfolio(Long userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        return portfolios.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public PortfolioDto addToPortfolio(Long userId, String stockSymbol, BigDecimal quantity, BigDecimal averagePrice) {
        // Validation
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("보유량은 0보다 커야 합니다.");
        }
        if (averagePrice == null || averagePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균가는 0보다 커야 합니다.");
        }

        User user = userService.getUserEntityById(userId);
        Stock stock = stockService.getStockEntityBySymbol(stockSymbol);

        if (stock.getCurrentPrice() == null || stock.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("주식 가격 정보가 없습니다.");
        }

        Portfolio portfolio = Portfolio.builder()
                .user(user)
                .stock(stock)
                .quantity(quantity)
                .averagePrice(averagePrice)
                .currentPrice(stock.getCurrentPrice())
                .build();

        updatePortfolioValues(portfolio);
        portfolio = portfolioRepository.save(portfolio);

        return convertToDto(portfolio);
    }

    public PortfolioDto updatePortfolio(Long portfolioId, BigDecimal quantity, BigDecimal averagePrice) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("포트폴리오를 찾을 수 없습니다."));

        // Validation
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("보유량은 0보다 커야 합니다.");
        }
        if (averagePrice != null && averagePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균가는 0보다 커야 합니다.");
        }

        if (quantity != null) portfolio.setQuantity(quantity);
        if (averagePrice != null) portfolio.setAveragePrice(averagePrice);

        // 현재가 업데이트
        portfolio.setCurrentPrice(portfolio.getStock().getCurrentPrice());
        updatePortfolioValues(portfolio);
        portfolio = portfolioRepository.save(portfolio);

        return convertToDto(portfolio);
    }

    public void deletePortfolio(Long portfolioId) {
        portfolioRepository.deleteById(portfolioId);
    }

    private void updatePortfolioValues(Portfolio portfolio) {
        if (portfolio.getCurrentPrice() != null && portfolio.getQuantity() != null && portfolio.getAveragePrice() != null) {
            portfolio.setTotalValue(portfolio.getCurrentPrice().multiply(portfolio.getQuantity()));
            BigDecimal profitLoss = portfolio.getCurrentPrice().subtract(portfolio.getAveragePrice())
                    .multiply(portfolio.getQuantity());
            portfolio.setProfitLoss(profitLoss);
            if (portfolio.getAveragePrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitLossRate = portfolio.getCurrentPrice().subtract(portfolio.getAveragePrice())
                        .divide(portfolio.getAveragePrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                portfolio.setProfitLossRate(profitLossRate);
            }
        }
    }

    public PortfolioDto convertToDto(Portfolio portfolio) {
        return PortfolioDto.builder()
                .id(portfolio.getId())
                .stock(stockService.convertToDto(portfolio.getStock()))
                .quantity(portfolio.getQuantity())
                .averagePrice(portfolio.getAveragePrice())
                .currentPrice(portfolio.getCurrentPrice())
                .totalValue(portfolio.getTotalValue())
                .profitLoss(portfolio.getProfitLoss())
                .profitLossRate(portfolio.getProfitLossRate())
                .build();
    }

    /**
     * AI 기반 포트폴리오 분석
     */
    public PortfolioAnalysisDto analyzePortfolio(Long userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        User user = userService.getUserEntityById(userId);

        // 포트폴리오 요약 생성
        StringBuilder summary = new StringBuilder();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (Portfolio p : portfolios) {
            totalValue = totalValue.add(p.getTotalValue() != null ? p.getTotalValue() : BigDecimal.ZERO);
            totalProfitLoss = totalProfitLoss.add(p.getProfitLoss() != null ? p.getProfitLoss() : BigDecimal.ZERO);

            summary.append(String.format(
                    "- %s (%s): 보유량 %s주, 평균가 %s원, 현재가 %s원, 손익 %s원 (%s%%)\n",
                    p.getStock().getName(),
                    p.getStock().getSymbol(),
                    p.getQuantity(),
                    p.getAveragePrice(),
                    p.getCurrentPrice(),
                    p.getProfitLoss(),
                    p.getProfitLossRate()
            ));
        }

        summary.append(String.format("\n총 평가액: %s원\n", totalValue));
        summary.append(String.format("총 손익: %s원\n", totalProfitLoss));

        // AI 분석 수행
        String investmentStyle = user.getInvestmentStyle() != null 
                ? user.getInvestmentStyle().toString() 
                : "BALANCED";
        String aiAnalysis = aiService.analyzePortfolio(summary.toString(), investmentStyle);

        return PortfolioAnalysisDto.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalValue.compareTo(BigDecimal.ZERO) > 0 
                        ? totalProfitLoss.divide(totalValue, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO)
                .analysis(aiAnalysis)
                .investmentStyle(investmentStyle)
                .build();
    }
}

