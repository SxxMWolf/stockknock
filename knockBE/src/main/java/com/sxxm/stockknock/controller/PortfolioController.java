package com.sxxm.stockknock.controller;

import com.sxxm.stockknock.dto.PortfolioDto;
import com.sxxm.stockknock.service.PortfolioService;
import com.sxxm.stockknock.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "http://localhost:3000")
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<PortfolioDto>> getPortfolio(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            List<PortfolioDto> portfolio = portfolioService.getUserPortfolio(userId);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<PortfolioDto> addToPortfolio(
            @RequestHeader("Authorization") String token,
            @RequestParam String stockSymbol,
            @RequestParam BigDecimal quantity,
            @RequestParam BigDecimal averagePrice) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            PortfolioDto portfolio = portfolioService.addToPortfolio(userId, stockSymbol, quantity, averagePrice);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDto> updatePortfolio(
            @PathVariable Long portfolioId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) BigDecimal averagePrice) {
        try {
            PortfolioDto portfolio = portfolioService.updatePortfolio(portfolioId, quantity, averagePrice);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable Long portfolioId) {
        try {
            portfolioService.deletePortfolio(portfolioId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

