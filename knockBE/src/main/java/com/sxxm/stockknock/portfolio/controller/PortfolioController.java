/**
 * 사용자 포트폴리오 관리 API. 조회, 추가, 수정, 삭제 및 AI 분석.
 */
package com.sxxm.stockknock.portfolio.controller;

import com.sxxm.stockknock.portfolio.dto.PortfolioDto;
import com.sxxm.stockknock.portfolio.dto.PortfolioAnalysisDto;
import com.sxxm.stockknock.portfolio.dto.AddPortfolioRequest;
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
    public ResponseEntity<?> getPortfolio(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.length() < 7) {
                System.err.println("Invalid token format: " + token);
                return ResponseEntity.status(401).build();
            }
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            List<PortfolioDto> portfolio = portfolioService.getUserPortfolio(userId);
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            System.err.println("포트폴리오 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addToPortfolio(
            @RequestHeader("Authorization") String token,
            @RequestBody AddPortfolioRequest request) {
        try {
            System.out.println("Received request: " + request);
            if (request == null) {
                System.err.println("Request body is null");
                return ResponseEntity.badRequest().body("Request body is null");
            }
            
            System.out.println("stockSymbol: " + request.getStockSymbol());
            System.out.println("quantity: " + request.getQuantity());
            System.out.println("avgBuyPrice: " + request.getAvgBuyPrice());
            
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            
            // 파라미터 검증
            if (request.getStockSymbol() == null || request.getStockSymbol().isEmpty()) {
                System.err.println("stockSymbol is null or empty");
                return ResponseEntity.badRequest().body("stockSymbol is required");
            }
            
            BigDecimal quantity;
            BigDecimal avgBuyPrice;
            try {
                quantity = request.getQuantityAsBigDecimal();
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    System.err.println("quantity is null or invalid: " + request.getQuantity());
                    return ResponseEntity.badRequest().body("quantity must be greater than 0");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid quantity format: " + request.getQuantity());
                return ResponseEntity.badRequest().body("Invalid quantity format");
            }
            
            try {
                avgBuyPrice = request.getAvgBuyPriceAsBigDecimal();
                if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    System.err.println("avgBuyPrice is null or invalid: " + request.getAvgBuyPrice());
                    return ResponseEntity.badRequest().body("avgBuyPrice must be greater than 0");
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid avgBuyPrice format: " + request.getAvgBuyPrice());
                return ResponseEntity.badRequest().body("Invalid avgBuyPrice format");
            }
            
            PortfolioDto portfolio = portfolioService.addToPortfolio(
                userId, 
                request.getStockSymbol(), 
                quantity, 
                avgBuyPrice
            );
            return ResponseEntity.ok(portfolio);
        } catch (Exception e) {
            System.err.println("Exception in addToPortfolio: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
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
    public ResponseEntity<?> analyzePortfolio(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false, defaultValue = "false") boolean forceRefresh) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            
            // forceRefresh가 true면 캐시 삭제 후 재분석
            if (forceRefresh) {
                portfolioService.clearPortfolioAnalysisCache(userId);
            }
            
            PortfolioAnalysisDto analysis = portfolioService.analyzePortfolio(userId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("포트폴리오 분석 오류: " + e.getMessage());
            // 에러 메시지를 포함한 기본 분석 결과 반환
            PortfolioAnalysisDto errorAnalysis = PortfolioAnalysisDto.builder()
                    .totalValue(java.math.BigDecimal.ZERO)
                    .totalProfitLoss(java.math.BigDecimal.ZERO)
                    .totalProfitLossRate(java.math.BigDecimal.ZERO)
                    .analysis("포트폴리오 분석 중 오류가 발생했습니다. FastAPI 서버를 확인해주세요.")
                    .investmentStyle("UNKNOWN")
                    .build();
            return ResponseEntity.ok(errorAnalysis);
        }
    }
}

