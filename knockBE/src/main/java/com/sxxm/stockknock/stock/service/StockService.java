package com.sxxm.stockknock.stock.service;

import com.sxxm.stockknock.stock.dto.StockDto;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.stock.entity.StockPriceHistory;
import com.sxxm.stockknock.stock.repository.StockRepository;
import com.sxxm.stockknock.stock.repository.StockPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
                   .volume(price.getVolume());
        }

        return builder.build();
    }

    @Autowired
    private com.sxxm.stockknock.common.service.FastApiService fastApiService;

    public BigDecimal getCurrentPrice(String symbol) {
        // FastAPI에서 주가 조회 시도
        try {
            BigDecimal price = fastApiService.getCurrentPrice(symbol).block();
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                return price;
            }
        } catch (Exception e) {
            System.err.println("FastAPI 주가 조회 실패, DB에서 조회: " + e.getMessage());
        }
        
        // FastAPI 실패 시 DB에서 조회
        Optional<StockPriceHistory> latestPrice = priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(symbol);
        return latestPrice.map(StockPriceHistory::getPrice).orElse(BigDecimal.ZERO);
    }

    public Stock getStockEntityBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다: " + symbol));
    }
}

