/**
 * 사용자 포트폴리오 관리 비즈니스 로직. 총 평가액, 손익, 수익률 계산 및 AI 분석.
 */
package com.sxxm.stockknock.portfolio.service;

import com.sxxm.stockknock.portfolio.dto.PortfolioDto;
import com.sxxm.stockknock.portfolio.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.portfolio.entity.Portfolio;
import com.sxxm.stockknock.portfolio.entity.PortfolioItem;
import com.sxxm.stockknock.portfolio.repository.PortfolioRepository;
import com.sxxm.stockknock.portfolio.repository.PortfolioItemRepository;
import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.stock.service.StockService;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.stock.dto.StockDto;
import com.sxxm.stockknock.stock.repository.StockRepository;
import com.sxxm.stockknock.ai.service.GPTClientService;
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
    private StockRepository stockRepository;
    
    @Autowired
    private GPTClientService gptClientService;

    @Autowired
    private PortfolioAnalysisService portfolioAnalysisService;

    public List<PortfolioDto> getUserPortfolio(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByPortfolioUserId(userId);
        
        // N+1 Query 방지: 모든 종목의 가격을 한 번에 조회 (IN 쿼리)
        List<String> symbols = items.stream()
                .map(item -> item.getStock() != null ? item.getStock().getSymbol() : null)
                .filter(s -> s != null)
                .distinct()
                .collect(Collectors.toList());
        
        // 가격 Map 생성 (한 번의 쿼리로 모든 가격 조회)
        Map<String, BigDecimal> priceMap = stockService.getCurrentPricesBatch(symbols);
        
        // DTO 변환 시 가격 Map 사용
        return items.stream()
                .map(item -> convertToDto(item, priceMap))
                .collect(Collectors.toList());
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
            // 포트폴리오 변경 → 분석 무효화 (다음 조회 시 재분석)
            invalidateAnalysisCache(userId);
            // 단일 아이템 변환 (가격 Map 없이 직접 조회)
            return convertToDtoSingle(existingItem);
        } else {
            // 새 종목 추가
            Stock stock = stockService.getStockEntityBySymbol(stockSymbol);
            
            // Stock의 name이 symbol과 같으면 종목명 업데이트 시도
            if (stock.getName() == null || stock.getName().equals(stockSymbol)) {
                // 종목명 매핑 (한국 주식)
                String stockName = getStockNameBySymbol(stockSymbol);
                if (stockName != null && !stockName.equals(stockSymbol)) {
                    stock.setName(stockName);
                    stock = stockRepository.save(stock);
                }
            }
            
            PortfolioItem item = PortfolioItem.builder()
                    .portfolio(portfolio)
                    .stock(stock)
                    .quantity(quantity)
                    .avgBuyPrice(avgBuyPrice)
                    .build();
            item = portfolioItemRepository.save(item);
            // 포트폴리오 변경 → 분석 무효화 (다음 조회 시 재분석)
            invalidateAnalysisCache(userId);
            // 단일 아이템 변환 (가격 Map 없이 직접 조회)
            return convertToDtoSingle(item);
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
        // 포트폴리오 변경 → 분석 무효화 (다음 조회 시 재분석)
        Long userId = item.getPortfolio().getUser().getId();
        invalidateAnalysisCache(userId);
        return convertToDtoSingle(item);
    }

    @Transactional
    public void deletePortfolio(Long itemId) {
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("포트폴리오 항목을 찾을 수 없습니다."));
        Long userId = item.getPortfolio().getUser().getId();
        portfolioItemRepository.deleteById(itemId);
        // 포트폴리오 변경 → 분석 무효화 (다음 조회 시 재분석)
        invalidateAnalysisCache(userId);
    }

    /**
     * 포트폴리오 분석 캐시 무효화 (포트폴리오 변경 시 호출)
     */
    private void invalidateAnalysisCache(Long userId) {
        // 해시를 변경하여 다음 조회 시 재분석되도록 함
        // 실제로는 PortfolioAnalysisService에서 해시 비교로 자동 처리되므로
        // 여기서는 로그만 남김
        org.slf4j.LoggerFactory.getLogger(PortfolioService.class)
                .debug("[포트폴리오 분석] 캐시 무효화 (userId={})", userId);
    }

    /**
     * PortfolioItem을 DTO로 변환 (가격 Map 사용, N+1 Query 방지)
     */
    private PortfolioDto convertToDto(PortfolioItem item, Map<String, BigDecimal> priceMap) {
        if (item.getStock() == null) {
            System.err.println("PortfolioItem의 stock이 null입니다: " + item.getId());
            throw new RuntimeException("Stock 정보가 없습니다.");
        }
        
        String symbol = item.getStock().getSymbol();
        BigDecimal currentPrice = priceMap.getOrDefault(symbol, BigDecimal.ZERO);
        
        BigDecimal totalValue = currentPrice.multiply(item.getQuantity());
        BigDecimal profitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                .multiply(item.getQuantity());
        BigDecimal profitLossRate = BigDecimal.ZERO;
        if (item.getAvgBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
            profitLossRate = currentPrice.subtract(item.getAvgBuyPrice())
                    .divide(item.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        StockDto stockDto = stockService.convertToDto(item.getStock());

        return PortfolioDto.builder()
                .id(item.getId())
                .portfolioId(item.getPortfolio().getId())
                .portfolioName(item.getPortfolio().getName())
                .stock(stockDto)
                .quantity(item.getQuantity())
                .avgBuyPrice(item.getAvgBuyPrice())
                .currentPrice(currentPrice)
                .totalValue(totalValue)
                .profitLoss(profitLoss)
                .profitLossRate(profitLossRate)
                .build();
    }
    
    /**
     * PortfolioItem을 DTO로 변환 (단일 아이템용, 가격 직접 조회)
     */
    private PortfolioDto convertToDtoSingle(PortfolioItem item) {
        if (item.getStock() == null) {
            System.err.println("PortfolioItem의 stock이 null입니다: " + item.getId());
            throw new RuntimeException("Stock 정보가 없습니다.");
        }
        
        String symbol = item.getStock().getSymbol();
        BigDecimal currentPrice = stockService.getCurrentPrice(symbol);
        if (currentPrice == null) {
            currentPrice = BigDecimal.ZERO;
        }
        
        BigDecimal totalValue = currentPrice.multiply(item.getQuantity());
        BigDecimal profitLoss = currentPrice.subtract(item.getAvgBuyPrice())
                .multiply(item.getQuantity());
        BigDecimal profitLossRate = BigDecimal.ZERO;
        if (item.getAvgBuyPrice().compareTo(BigDecimal.ZERO) > 0) {
            profitLossRate = currentPrice.subtract(item.getAvgBuyPrice())
                    .divide(item.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        StockDto stockDto = stockService.convertToDto(item.getStock());

        return PortfolioDto.builder()
                .id(item.getId())
                .portfolioId(item.getPortfolio().getId())
                .portfolioName(item.getPortfolio().getName())
                .stock(stockDto)
                .quantity(item.getQuantity())
                .avgBuyPrice(item.getAvgBuyPrice())
                .currentPrice(currentPrice)
                .totalValue(totalValue)
                .profitLoss(profitLoss)
                .profitLossRate(profitLossRate)
                .build();
    }

    /**
     * AI 기반 포트폴리오 분석 (캐싱 로직 포함)
     * PortfolioAnalysisService로 위임
     */
    public PortfolioAnalysisDto analyzePortfolio(Long userId) {
        return portfolioAnalysisService.getPortfolioAnalysis(userId);
    }

    /**
     * 포트폴리오 분석 캐시 삭제 (강제 재분석용)
     */
    @Transactional
    public void clearPortfolioAnalysisCache(Long userId) {
        portfolioAnalysisService.clearCache(userId);
    }

    /**
     * 종목 코드로 종목명 조회 (한국 주식)
     */
    private String getStockNameBySymbol(String symbol) {
        Map<String, String> stockNameMap = Map.of(
            "005930", "삼성전자",
            "000660", "SK하이닉스",
            "035420", "NAVER",
            "035720", "카카오",
            "005380", "현대자동차",
            "066570", "LG전자",
            "068270", "셀트리온",
            "005490", "POSCO홀딩스"
        );
        return stockNameMap.getOrDefault(symbol, symbol);
    }
}
