package com.sxxm.stockknock.service;

import com.sxxm.stockknock.dto.PortfolioDto;
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

    public List<PortfolioDto> getUserPortfolio(Long userId) {
        List<Portfolio> portfolios = portfolioRepository.findByUserId(userId);
        return portfolios.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public PortfolioDto addToPortfolio(Long userId, String stockSymbol, BigDecimal quantity, BigDecimal averagePrice) {
        User user = userService.getUserEntityById(userId);
        Stock stock = stockService.getStockEntityBySymbol(stockSymbol);

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

        if (quantity != null) portfolio.setQuantity(quantity);
        if (averagePrice != null) portfolio.setAveragePrice(averagePrice);

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
}

