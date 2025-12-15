package com.sxxm.stockknock.portfolio.controller;

import com.sxxm.stockknock.portfolio.dto.PortfolioDto;
import com.sxxm.stockknock.portfolio.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.portfolio.service.PortfolioService;
import com.sxxm.stockknock.common.util.JwtUtil;
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
            @RequestParam BigDecimal avgBuyPrice) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            PortfolioDto portfolio = portfolioService.addToPortfolio(userId, stockSymbol, quantity, avgBuyPrice);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioDto> updatePortfolio(
            @RequestHeader("Authorization") String token,
            @PathVariable Long portfolioId,
            @RequestParam(required = false) BigDecimal quantity,
            @RequestParam(required = false) BigDecimal avgBuyPrice) {
        try {
            jwtUtil.getUserIdFromToken(token.substring(7)); // 인증 확인
            PortfolioDto portfolio = portfolioService.updatePortfolio(portfolioId, quantity, avgBuyPrice);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(
            @RequestHeader("Authorization") String token,
            @PathVariable Long portfolioId) {
        try {
            jwtUtil.getUserIdFromToken(token.substring(7)); // 인증 확인
            portfolioService.deletePortfolio(portfolioId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/analysis")
    public ResponseEntity<PortfolioAnalysisDto> analyzePortfolio(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            PortfolioAnalysisDto analysis = portfolioService.analyzePortfolio(userId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

