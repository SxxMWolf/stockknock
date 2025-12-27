/**
 * 주식 정보 조회 및 관리. 현재가 조회(DB 우선), 배치 가격 조회, 장중 시간 체크.
 */
package com.sxxm.stockknock.stock.service;

import com.sxxm.stockknock.stock.dto.StockDto;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.stock.entity.StockPriceHistory;
import com.sxxm.stockknock.stock.repository.StockRepository;
import com.sxxm.stockknock.stock.repository.StockPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceHistoryRepository priceHistoryRepository;
    
    /**
     * 여러 종목의 최신 가격을 한 번에 조회 (N+1 Query 방지)
     * @param symbols 종목 심볼 리스트
     * @return symbol -> price Map
     */
    public Map<String, BigDecimal> getCurrentPricesBatch(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashMap<>();
        }
        
        // DB에서 한 번에 조회
        List<Object[]> results = priceHistoryRepository.findLatestPricesBySymbols(symbols);
        
        // Map으로 변환
        Map<String, BigDecimal> priceMap = results.stream()
                .collect(Collectors.toMap(
                    row -> (String) row[0],  // symbol
                    row -> {
                        Object priceObj = row[1];
                        if (priceObj instanceof BigDecimal) {
                            return (BigDecimal) priceObj;
                        } else if (priceObj instanceof Number) {
                            return BigDecimal.valueOf(((Number) priceObj).doubleValue());
                        }
                        return BigDecimal.ZERO;
                    },
                    (existing, replacement) -> existing  // 중복 시 기존 값 유지
                ));
        
        // DB에 없는 종목은 0으로 설정
        for (String symbol : symbols) {
            priceMap.putIfAbsent(symbol, BigDecimal.ZERO);
        }
        
        return priceMap;
    }

    @Autowired
    private StockPriceService stockPriceService;

    public StockDto getStockBySymbol(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다: " + symbol));
        return convertToDto(stock);
    }

    public List<StockDto> searchStocks(String keyword) {
        List<Stock> stocks = stockRepository.findByNameContaining(keyword);
        return stocks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<StockDto> getStocksByCountry(String country) {
        List<Stock> stocks = stockRepository.findByCountry(country);
        return stocks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<StockDto> getStocksByIndustry(String industry) {
        List<Stock> stocks = stockRepository.findByIndustry(industry);
        return stocks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Autowired
    private com.sxxm.stockknock.common.service.FastApiService fastApiService;

    public StockDto convertToDto(Stock stock) {
        StockDto.StockDtoBuilder builder = StockDto.builder()
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .exchange(stock.getExchange())
                .country(stock.getCountry())
                .industry(stock.getIndustry())
                .currency(stock.getCurrency());

        // 최신 가격 정보 가져오기
        Optional<StockPriceHistory> latestPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(stock.getSymbol());
        if (latestPrice.isPresent()) {
            StockPriceHistory price = latestPrice.get();
            builder.currentPrice(price.getPrice())
                   .dayHigh(price.getHigh())
                   .dayLow(price.getLow())
                   .volume(price.getVolume())
                   .lastUpdated(price.getTimestamp()); // 업데이트 시점 추가
            
            // 전일 종가 조회 (최신 가격 이력이 2개 이상이면 두 번째 것을 전일 종가로 사용)
            List<StockPriceHistory> priceHistory = priceHistoryRepository.findByStockSymbolOrderByTimestampDesc(
                stock.getSymbol(), 
                PageRequest.of(0, 2)
            );
            if (priceHistory.size() >= 2) {
                builder.previousClose(priceHistory.get(1).getPrice());
            } else {
                builder.previousClose(price.getPrice()); // 전일 데이터가 없으면 현재 가격으로 설정
            }
        } else {
            // DB에 가격 이력이 없으면 null로 반환 (빠른 응답을 위해 FastAPI 호출 제거)
            // 가격은 스케줄러가 주기적으로 업데이트하므로, 여기서는 즉시 반환
            // 성능 최적화: FastAPI 호출을 동기적으로 하지 않음
        }

        return builder.build();
    }


    /**
     * 한국 주식 시장 장중 시간 체크 (09:00 ~ 15:30 KST)
     */
    private boolean isMarketHours() {
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime marketOpen = LocalTime.of(9, 0);
        LocalTime marketClose = LocalTime.of(15, 30);
        return !now.isBefore(marketOpen) && !now.isAfter(marketClose);
    }

    /**
     * 가격 조회 (DB 우선, 장중에는 외부 API 호출 금지)
     * 가격 조회 실패 시 null 반환 (BigDecimal.ZERO 대신)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        // DB에서 먼저 조회
        Optional<StockPriceHistory> latestPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(symbol);
        if (latestPrice.isPresent()) {
            return latestPrice.get().getPrice();
        }
        
        System.out.println("[PRICE] DB miss: " + symbol);
        
        // 장중 시간 체크: 장중일 때는 외부 API 호출 금지 (캐시만 사용)
        if (isMarketHours()) {
            System.out.println("[PRICE] Market hours: external API call disabled for " + symbol);
            return null;
        }
        
        // DB에 없으면 Yahoo Finance API 시도 (더 안정적)
        try {
            stockPriceService.updateStockPriceFromYahooFinance(symbol);
            
            // 저장 후 다시 조회
            Optional<StockPriceHistory> updatedPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(symbol);
            if (updatedPrice.isPresent() && updatedPrice.get().getPrice().compareTo(BigDecimal.ZERO) > 0) {
                return updatedPrice.get().getPrice();
            }
        } catch (Exception e) {
            // Yahoo Finance 실패 시 로그 없음 (FastAPI로 진행)
        }
        
        // Yahoo Finance 실패 시 FastAPI 시도 (폴백)
        try {
            BigDecimal price = fastApiService.getCurrentPrice(symbol).block();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                // DB에 저장
                Stock stock = stockRepository.findBySymbol(symbol).orElse(null);
                if (stock != null) {
                    StockPriceHistory priceHistory = StockPriceHistory.builder()
                            .stock(stock)
                            .price(price)
                            .timestamp(LocalDateTime.now())
                            .build();
                    priceHistoryRepository.save(priceHistory);
                }
                return price;
            }
        } catch (Exception e) {
            // FastAPI 실패 시 로그는 FastAPI에서 처리
        }
        
        System.out.println("[PRICE] External API failed: " + symbol);
        System.out.println("[PRICE] Final result: null");
        return null;  // null 반환 (BigDecimal.ZERO 대신)
    }

    public Stock getStockEntityBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseGet(() -> {
                    // Stock이 없으면 기본 정보로 생성
                    System.out.println("Stock이 없어서 생성합니다: " + symbol);
                    Stock newStock = Stock.builder()
                            .symbol(symbol)
                            .name(symbol) // 기본값으로 symbol 사용, 나중에 업데이트 가능
                            .country("KR")
                            .currency("KRW")
                            .build();
                    return stockRepository.save(newStock);
                });
    }
}

