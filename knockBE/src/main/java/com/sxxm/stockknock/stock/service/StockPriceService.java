package com.sxxm.stockknock.stock.service;

import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.stock.entity.StockPriceHistory;
import com.sxxm.stockknock.stock.repository.StockRepository;
import com.sxxm.stockknock.stock.repository.StockPriceHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 주식 가격 업데이트 서비스
 * 
 * 역할:
 * - 외부 API를 통한 주식 가격 업데이트
 * - Yahoo Finance, Alpha Vantage, Twelve Data 지원
 * - 가격 이력을 DB에 저장 (StockPriceHistory)
 * - 모든 종목 가격 일괄 업데이트
 * - 스케줄러에서 호출되어 장 종료 후 가격 갱신
 */
@Service
public class StockPriceService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceHistoryRepository priceHistoryRepository;

    @Value("${stock.api.alpha-vantage.key:}")
    private String alphaVantageKey;

    @Value("${stock.api.yahoo-finance.enabled:true}")
    private boolean yahooFinanceEnabled;

    @Value("${stock.api.twelve-data.key:}")
    private String twelveDataKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://query1.finance.yahoo.com")
            .build();

    /**
     * 한국 주식 심볼을 Yahoo Finance 형식으로 변환 (예: 005930 -> 005930.KS)
     */
    private String convertKoreanSymbol(String symbol) {
        if (symbol != null && symbol.matches("\\d{6}")) {
            return symbol + ".KS";
        }
        return symbol;
    }

    /**
     * Yahoo Finance API를 통해 주식 가격 업데이트
     * 무료 API, 제한: 초당 2회 요청
     */
    public void updateStockPriceFromYahooFinance(String symbol) {
        try {
            // 한국 주식 심볼 변환
            String yahooSymbol = convertKoreanSymbol(symbol);
            String url = String.format("/v8/finance/chart/%s?interval=1d&range=1d", yahooSymbol);
            
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("chart")) {
                Map<String, Object> chart = (Map<String, Object>) response.get("chart");
                List<Map<String, Object>> result = (List<Map<String, Object>>) chart.get("result");
                
                if (result != null && !result.isEmpty()) {
                    Map<String, Object> quote = (Map<String, Object>) result.get(0);
                    Map<String, Object> meta = (Map<String, Object>) quote.get("meta");
                    
                    if (meta != null) {
                        Stock stock = stockRepository.findBySymbol(symbol)
                                .orElse(null);
                        
                        if (stock != null) {
                            // 타입 안전하게 값 추출
                            Number priceNum = (Number) meta.get("regularMarketPrice");
                            Number prevCloseNum = (Number) meta.get("previousClose");
                            Number highNum = (Number) meta.get("regularMarketDayHigh");
                            Number lowNum = (Number) meta.get("regularMarketDayLow");
                            Number volumeNum = (Number) meta.get("regularMarketVolume");
                            
                            if (priceNum != null) {
                                BigDecimal price = BigDecimal.valueOf(priceNum.doubleValue());
                                BigDecimal previousClose = prevCloseNum != null ? BigDecimal.valueOf(prevCloseNum.doubleValue()) : null;
                                BigDecimal dayHigh = highNum != null ? BigDecimal.valueOf(highNum.doubleValue()) : null;
                                BigDecimal dayLow = lowNum != null ? BigDecimal.valueOf(lowNum.doubleValue()) : null;
                                Long volume = volumeNum != null ? volumeNum.longValue() : null;
                                
                                // StockPriceHistory에 저장
                                StockPriceHistory priceHistory = StockPriceHistory.builder()
                                        .stock(stock)
                                        .price(price)
                                        .open(previousClose)
                                        .high(dayHigh)
                                        .low(dayLow)
                                        .volume(volume)
                                        .timestamp(LocalDateTime.now())
                                        .build();
                                priceHistoryRepository.save(priceHistory);
                                System.out.println("Yahoo Finance 가격 저장 성공: " + symbol + " = " + price);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Yahoo Finance API 오류: " + symbol + " - " + e.getMessage());
        }
    }

    /**
     * Alpha Vantage API를 통해 주식 가격 업데이트
     * 무료 API, 제한: 일일 25회 요청, 분당 5회 요청
     */
    public void updateStockPriceFromAlphaVantage(String symbol) {
        if (alphaVantageKey == null || alphaVantageKey.isEmpty()) {
            return;
        }

        try {
            String url = String.format(
                    "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                    symbol, alphaVantageKey
            );

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("Global Quote")) {
                Map<String, String> quote = (Map<String, String>) response.get("Global Quote");
                
                Stock stock = stockRepository.findBySymbol(symbol)
                        .orElse(null);
                
                if (stock != null && quote != null) {
                    String price = quote.get("05. price");
                    String previousClose = quote.get("08. previous close");
                    String high = quote.get("03. high");
                    String low = quote.get("04. low");
                    String volume = quote.get("06. volume");
                    
                    if (price != null && !price.isEmpty()) {
                        // StockPriceHistory에 저장
                        StockPriceHistory priceHistory = StockPriceHistory.builder()
                                .stock(stock)
                                .price(new BigDecimal(price))
                                .open(previousClose != null && !previousClose.isEmpty() ? new BigDecimal(previousClose) : null)
                                .high(high != null && !high.isEmpty() ? new BigDecimal(high) : null)
                                .low(low != null && !low.isEmpty() ? new BigDecimal(low) : null)
                                .volume(volume != null && !volume.isEmpty() ? Long.parseLong(volume) : null)
                                .timestamp(LocalDateTime.now())
                                .build();
                        priceHistoryRepository.save(priceHistory);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Alpha Vantage API 오류: " + symbol + " - " + e.getMessage());
        }
    }

    /**
     * Twelve Data API를 통해 주식 가격 업데이트
     * 무료 API, 제한: 일일 800회 요청
     */
    public void updateStockPriceFromTwelveData(String symbol) {
        if (twelveDataKey == null || twelveDataKey.isEmpty()) {
            return;
        }

        try {
            String url = String.format(
                    "https://api.twelvedata.com/price?symbol=%s&apikey=%s",
                    symbol, twelveDataKey
            );

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("price")) {
                String priceStr = (String) response.get("price");
                
                Stock stock = stockRepository.findBySymbol(symbol)
                        .orElse(null);
                
                if (stock != null && priceStr != null) {
                    // StockPriceHistory에 저장
                    StockPriceHistory priceHistory = StockPriceHistory.builder()
                            .stock(stock)
                            .price(new BigDecimal(priceStr))
                            .timestamp(LocalDateTime.now())
                            .build();
                    priceHistoryRepository.save(priceHistory);
                }
            }
        } catch (Exception e) {
            System.err.println("Twelve Data API 오류: " + symbol + " - " + e.getMessage());
        }
    }

    /**
     * 모든 주식 가격 업데이트 (우선순위: Yahoo Finance > Alpha Vantage > Twelve Data)
     */
    public void updateAllStockPrices() {
        List<Stock> stocks = stockRepository.findAll();
        
        for (Stock stock : stocks) {
            try {
                if (yahooFinanceEnabled) {
                    updateStockPriceFromYahooFinance(stock.getSymbol());
                    Thread.sleep(500); // API 제한 방지 (초당 2회)
                } else if (alphaVantageKey != null && !alphaVantageKey.isEmpty()) {
                    updateStockPriceFromAlphaVantage(stock.getSymbol());
                    Thread.sleep(12000); // API 제한 방지 (분당 5회)
                } else if (twelveDataKey != null && !twelveDataKey.isEmpty()) {
                    updateStockPriceFromTwelveData(stock.getSymbol());
                    Thread.sleep(100); // API 제한 방지
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("주식 가격 업데이트 오류: " + stock.getSymbol() + " - " + e.getMessage());
            }
        }
    }
}

