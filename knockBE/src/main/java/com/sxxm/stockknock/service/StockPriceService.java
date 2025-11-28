package com.sxxm.stockknock.service;

import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 외부 API를 통해 주식 가격을 업데이트하는 서비스
 * Yahoo Finance, Alpha Vantage, Twelve Data 등 지원
 */
@Service
public class StockPriceService {

    @Autowired
    private StockRepository stockRepository;

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
     * Yahoo Finance API를 통해 주식 가격 업데이트
     * 무료 API, 제한: 초당 2회 요청
     */
    public void updateStockPriceFromYahooFinance(String symbol) {
        try {
            // Yahoo Finance API 호출
            // 예: https://query1.finance.yahoo.com/v8/finance/chart/AAPL
            String url = String.format("/v8/finance/chart/%s?interval=1d&range=1d", symbol);
            
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
                            Double regularMarketPrice = (Double) meta.get("regularMarketPrice");
                            Double previousClose = (Double) meta.get("previousClose");
                            Double dayHigh = (Double) meta.get("regularMarketDayHigh");
                            Double dayLow = (Double) meta.get("regularMarketDayLow");
                            Long volume = ((Double) meta.get("regularMarketVolume")).longValue();
                            Double marketCap = (Double) meta.get("marketCap");
                            
                            if (regularMarketPrice != null) {
                                stock.setCurrentPrice(BigDecimal.valueOf(regularMarketPrice));
                            }
                            if (previousClose != null) {
                                stock.setPreviousClose(BigDecimal.valueOf(previousClose));
                            }
                            if (dayHigh != null) {
                                stock.setDayHigh(BigDecimal.valueOf(dayHigh));
                            }
                            if (dayLow != null) {
                                stock.setDayLow(BigDecimal.valueOf(dayLow));
                            }
                            if (volume != null) {
                                stock.setVolume(volume);
                            }
                            if (marketCap != null) {
                                stock.setMarketCap(BigDecimal.valueOf(marketCap));
                            }
                            
                            stockRepository.save(stock);
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
                        stock.setCurrentPrice(new BigDecimal(price));
                    }
                    if (previousClose != null && !previousClose.isEmpty()) {
                        stock.setPreviousClose(new BigDecimal(previousClose));
                    }
                    if (high != null && !high.isEmpty()) {
                        stock.setDayHigh(new BigDecimal(high));
                    }
                    if (low != null && !low.isEmpty()) {
                        stock.setDayLow(new BigDecimal(low));
                    }
                    if (volume != null && !volume.isEmpty()) {
                        stock.setVolume(Long.parseLong(volume));
                    }
                    
                    stockRepository.save(stock);
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
                    stock.setCurrentPrice(new BigDecimal(priceStr));
                    stockRepository.save(stock);
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

