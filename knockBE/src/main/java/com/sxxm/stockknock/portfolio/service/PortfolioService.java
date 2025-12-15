package com.sxxm.stockknock.portfolio.service;

import com.sxxm.stockknock.common.service.FastApiService;
import com.sxxm.stockknock.portfolio.dto.PortfolioDto;
import com.sxxm.stockknock.portfolio.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.portfolio.entity.Portfolio;
import com.sxxm.stockknock.portfolio.entity.PortfolioItem;
import com.sxxm.stockknock.portfolio.repository.PortfolioRepository;
import com.sxxm.stockknock.portfolio.repository.PortfolioItemRepository;
import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PortfolioItemRepository portfolioItemRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StockService stockService;

    @Autowired
    private FastApiService fastApiService;

    public List<PortfolioDto> getUserPortfolio(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioUserId(userId);
        return items.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public PortfolioDto addToPortfolio(Long userId, String stockSymbol, BigDecimal quantity, BigDecimal avgBuyPrice) {
        // Validation
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("보유량은 0보다 커야 합니다.");
        }
        if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균가는 0보다 커야 합니다.");
        }

        User user = userService.getUserEntityById(userId);
        
        // 기본 포트폴리오 가져오기 또는 생성
        Portfolio portfolio = portfolioRepository.findByUserId(userId).stream()
                .findFirst()
                .orElseGet(() -> {
                    Portfolio newPortfolio = Portfolio.builder()
                            .user(user)
                            .name("Default Portfolio")
                            .build();
                    return portfolioRepository.save(newPortfolio);
                });

        // 기존 종목이 있는지 확인
        PortfolioItem existingItem = portfolioItemRepository
                .findByPortfolioIdAndStockSymbol(portfolio.getId(), stockSymbol)
                .orElse(null);

        if (existingItem != null) {
            // 기존 종목 업데이트 (수량과 평균가 재계산)
            BigDecimal totalQuantity = existingItem.getQuantity().add(quantity);
            BigDecimal totalCost = existingItem.getAvgBuyPrice().multiply(existingItem.getQuantity())
                    .add(avgBuyPrice.multiply(quantity));
            BigDecimal newAvgPrice = totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP);
            
            existingItem.setQuantity(totalQuantity);
            existingItem.setAvgBuyPrice(newAvgPrice);
            existingItem = portfolioItemRepository.save(existingItem);
            return convertToDto(existingItem);
        } else {
            // 새 종목 추가
            PortfolioItem item = PortfolioItem.builder()
                    .portfolio(portfolio)
                    .stock(stockService.getStockEntityBySymbol(stockSymbol))
                    .quantity(quantity)
                    .avgBuyPrice(avgBuyPrice)
                    .build();
            item = portfolioItemRepository.save(item);
            return convertToDto(item);
        }
    }

    @Transactional
    public PortfolioDto updatePortfolio(Long itemId, BigDecimal quantity, BigDecimal avgBuyPrice) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("포트폴리오 항목을 찾을 수 없습니다."));

        // Validation
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("보유량은 0보다 커야 합니다.");
        }
        if (avgBuyPrice != null && avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균가는 0보다 커야 합니다.");
        }

        if (quantity != null) item.setQuantity(quantity);
        if (avgBuyPrice != null) item.setAvgBuyPrice(avgBuyPrice);
        
        item = portfolioItemRepository.save(item);
        return convertToDto(item);
    }

    @Transactional
    public void deletePortfolio(Long itemId) {
        portfolioItemRepository.deleteById(itemId);
    }

    private PortfolioDto convertToDto(PortfolioItem item) {
        BigDecimal currentPrice = stockService.getCurrentPrice(item.getStock().getSymbol());
        BigDecimal totalValue = currentPrice.multiply(item.getQuantity());
        BigDecimal profitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                .multiply(item.getQuantity());
        BigDecimal profitLossRate = BigDecimal.ZERO;
        if (item.getAvgBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
            profitLossRate = currentPrice.subtract(item.getAvgBuyPrice())
                    .divide(item.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return PortfolioDto.builder()
                .id(item.getId())
                .portfolioId(item.getPortfolio().getId())
                .portfolioName(item.getPortfolio().getName())
                .stock(stockService.convertToDto(item.getStock()))
                .quantity(item.getQuantity())
                .avgBuyPrice(item.getAvgBuyPrice())
                .currentPrice(currentPrice)
                .totalValue(totalValue)
                .profitLoss(profitLoss)
                .profitLossRate(profitLossRate)
                .build();
    }

    /**
     * AI 기반 포트폴리오 분석
     */
    public PortfolioAnalysisDto analyzePortfolio(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioUserId(userId);

        // 포트폴리오 요약 생성
        StringBuilder summary = new StringBuilder();
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalProfitLoss = BigDecimal.ZERO;

        for (PortfolioItem item : items) {
            BigDecimal currentPrice = stockService.getCurrentPrice(item.getStock().getSymbol());
            BigDecimal itemValue = currentPrice.multiply(item.getQuantity());
            BigDecimal itemProfitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                    .multiply(item.getQuantity());
            
            totalValue = totalValue.add(itemValue);
            totalProfitLoss = totalProfitLoss.add(itemProfitLoss);

            summary.append(String.format(
                    "- %s (%s): 보유량 %s주, 평균가 %s원, 현재가 %s원, 손익 %s원\n",
                    item.getStock().getName(),
                    item.getStock().getSymbol(),
                    item.getQuantity(),
                    item.getAvgBuyPrice(),
                    currentPrice,
                    itemProfitLoss
            ));
        }

        summary.append(String.format("\n총 평가액: %s원\n", totalValue));
        summary.append(String.format("총 손익: %s원\n", totalProfitLoss));

        // FastAPI에서 AI 분석 수행
        String aiAnalysis = "포트폴리오 분석을 준비 중입니다.";
        try {
            Map<String, Object> request = Map.of(
                "portfolio_summary", summary.toString(),
                "user_investment_style", "BALANCED"
            );
            
            Map<String, Object> response = fastApiService.analyzePortfolio(request).block();
            if (response != null && response.containsKey("analysis")) {
                aiAnalysis = (String) response.get("analysis");
            }
        } catch (Exception e) {
            System.err.println("FastAPI 포트폴리오 분석 실패: " + e.getMessage());
            aiAnalysis = "포트폴리오 분석 중 오류가 발생했습니다.";
        }

        return PortfolioAnalysisDto.builder()
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalValue.compareTo(BigDecimal.ZERO) > 0 
                        ? totalProfitLoss.divide(totalValue, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO)
                .analysis(aiAnalysis)
                .investmentStyle("BALANCED")
                .build();
    }
}
