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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceHistoryRepository priceHistoryRepository;

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
            System.out.println("DB에 가격 이력 없음 (즉시 반환): " + stock.getSymbol() + " - 스케줄러가 나중에 업데이트");
        }

        return builder.build();
    }

    /**
     * FastAPI에서 가격을 가져와서 DB에 저장
     * 주의: 현재는 사용하지 않음 (성능 최적화를 위해 제거)
     * 필요시 스케줄러나 백그라운드 작업에서 사용
     */
    @SuppressWarnings("unused")
    private BigDecimal getCurrentPriceAndSave(Stock stock) {
        // FastAPI에서 주가 조회 시도
        try {
            System.out.println("FastAPI 호출 시작: " + stock.getSymbol());
            BigDecimal price = fastApiService.getCurrentPrice(stock.getSymbol()).block();
            System.out.println("FastAPI 응답: " + stock.getSymbol() + " = " + price);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                // DB에 저장
                StockPriceHistory priceHistory = StockPriceHistory.builder()
                        .stock(stock)
                        .price(price)
                        .timestamp(LocalDateTime.now())
                        .build();
                priceHistoryRepository.save(priceHistory);
                System.out.println("가격 DB 저장 완료: " + stock.getSymbol() + " = " + price);
                return price;
            } else {
                System.out.println("FastAPI에서 가격이 0이거나 null: " + stock.getSymbol());
            }
        } catch (Exception e) {
            System.err.println("FastAPI 주가 조회 실패: " + stock.getSymbol() + " - " + e.getMessage());
            e.printStackTrace();
        }
        
        // FastAPI 실패 시 DB에서 조회
        Optional<StockPriceHistory> latestPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(stock.getSymbol());
        if (latestPrice.isPresent()) {
            System.out.println("DB에서 가격 조회 성공: " + stock.getSymbol() + " = " + latestPrice.get().getPrice());
            return latestPrice.get().getPrice();
        }
        System.out.println("DB에도 가격 이력 없음: " + stock.getSymbol());
        return BigDecimal.ZERO;
    }

    /**
     * 가격 조회 (DB 우선, 없으면 FastAPI 호출)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        // DB에서 먼저 조회
        Optional<StockPriceHistory> latestPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(symbol);
        if (latestPrice.isPresent()) {
            return latestPrice.get().getPrice();
        }
        
        // DB에 없으면 FastAPI에서 조회 (하지만 저장은 하지 않음, convertToDto에서 처리)
        try {
            BigDecimal price = fastApiService.getCurrentPrice(symbol).block();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return price;
            }
        } catch (Exception e) {
            System.err.println("FastAPI 주가 조회 실패: " + symbol + " - " + e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }

    public Stock getStockEntityBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다: " + symbol));
    }
}

