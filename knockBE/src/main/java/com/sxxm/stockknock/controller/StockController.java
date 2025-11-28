package com.sxxm.stockknock.controller;

import com.sxxm.stockknock.dto.StockDto;
import com.sxxm.stockknock.service.StockService;
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

    @GetMapping("/industry/{industryId}")
    public ResponseEntity<List<StockDto>> getStocksByIndustry(@PathVariable Long industryId) {
        List<StockDto> stocks = stockService.getStocksByIndustry(industryId);
        return ResponseEntity.ok(stocks);
    }
}

