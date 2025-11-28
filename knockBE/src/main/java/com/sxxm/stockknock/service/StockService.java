package com.sxxm.stockknock.service;

import com.sxxm.stockknock.dto.StockDto;
import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

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

    public List<StockDto> getStocksByIndustry(Long industryId) {
        List<Stock> stocks = stockRepository.findByIndustryId(industryId);
        return stocks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public StockDto convertToDto(Stock stock) {
        return StockDto.builder()
                .id(stock.getId())
                .symbol(stock.getSymbol())
                .name(stock.getName())
                .exchange(stock.getExchange())
                .country(stock.getCountry())
                .currentPrice(stock.getCurrentPrice())
                .previousClose(stock.getPreviousClose())
                .dayHigh(stock.getDayHigh())
                .dayLow(stock.getDayLow())
                .volume(stock.getVolume())
                .marketCap(stock.getMarketCap())
                .peRatio(stock.getPeRatio())
                .dividendYield(stock.getDividendYield())
                .build();
    }

    public Stock getStockEntity(Long stockId) {
        return stockRepository.findById(stockId)
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다."));
    }

    public Stock getStockEntityBySymbol(String symbol) {
        return stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new RuntimeException("주식을 찾을 수 없습니다: " + symbol));
    }
}

