/**
 * 주식 정보 조회 API. 종목 검색 및 상세 정보 조회.
 */
package com.sxxm.stockknock.stock.controller;

import com.sxxm.stockknock.stock.dto.StockDto;
import com.sxxm.stockknock.stock.service.StockService;
import com.sxxm.stockknock.stock.service.StockPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@CrossOrigin(origins = "http://localhost:3000")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockPriceService stockPriceService;

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<StockDto> getStockBySymbol(@PathVariable String symbol) {
        try {
            StockDto stock = stockService.getStockBySymbol(symbol);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<StockDto>> searchStocks(@RequestParam String keyword) {
        List<StockDto> stocks = stockService.searchStocks(keyword);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<StockDto>> getStocksByCountry(@PathVariable String country) {
        List<StockDto> stocks = stockService.getStocksByCountry(country);
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/industry/{industry}")
    public ResponseEntity<List<StockDto>> getStocksByIndustry(@PathVariable String industry) {
        List<StockDto> stocks = stockService.getStocksByIndustry(industry);
        return ResponseEntity.ok(stocks);
    }

    /**
     * 특정 종목의 가격을 수동으로 업데이트
     */
    @PostMapping("/{symbol}/update-price")
    public ResponseEntity<String> updateStockPrice(@PathVariable String symbol) {
        try {
            stockPriceService.updateStockPriceFromYahooFinance(symbol);
            return ResponseEntity.ok("가격 업데이트 완료: " + symbol);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("가격 업데이트 실패: " + e.getMessage());
        }
    }
}

