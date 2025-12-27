/**
 * 사용자 관심 종목 관리 API. 조회, 추가, 삭제.
 */
package com.sxxm.stockknock.watchlist.controller;

import com.sxxm.stockknock.stock.dto.StockDto;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.stock.repository.StockRepository;
import com.sxxm.stockknock.watchlist.entity.Watchlist;
import com.sxxm.stockknock.watchlist.repository.WatchlistRepository;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.stock.service.StockService;
import com.sxxm.stockknock.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/watchlist")
@CrossOrigin(origins = "http://localhost:3000")
public class WatchlistController {

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.sxxm.stockknock.stock.service.StockPriceService stockPriceService;

    @GetMapping
    public ResponseEntity<?> getWatchlist(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.length() < 7) {
                System.err.println("Invalid token format: " + token);
                return ResponseEntity.status(401).build();
            }
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            List<Watchlist> watchlists = watchlistRepository.findByUserId(userId);
            List<StockDto> stocks = watchlists.stream()
                    .map(w -> stockService.convertToDto(w.getStock()))
                    .collect(Collectors.toList());
            
            // 백그라운드에서 가격이 없는 종목들의 가격 업데이트 (비동기)
            new Thread(() -> {
                for (StockDto stock : stocks) {
                    if (stock.getCurrentPrice() == null || stock.getCurrentPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                        try {
                            System.out.println("백그라운드 가격 업데이트 시작: " + stock.getSymbol());
                            stockPriceService.updateStockPriceFromYahooFinance(stock.getSymbol());
                            Thread.sleep(500); // API 제한 방지
                        } catch (Exception e) {
                            System.err.println("백그라운드 가격 업데이트 실패: " + stock.getSymbol() + " - " + e.getMessage());
                        }
                    }
                }
            }).start();
            
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            System.err.println("관심 종목 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{stockSymbol}")
    public ResponseEntity<Void> addToWatchlist(
            @RequestHeader("Authorization") String token,
            @PathVariable String stockSymbol) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            var user = userService.getUserEntityById(userId);
            
            // 종목이 DB에 없으면 기본 정보로 생성
            Stock stock = stockRepository.findBySymbol(stockSymbol).orElseGet(() -> {
                // 기본 종목 정보로 생성 (이름은 심볼로 임시 설정, 나중에 업데이트 가능)
                Stock newStock = Stock.builder()
                        .symbol(stockSymbol)
                        .name(stockSymbol) // 임시 이름, 나중에 업데이트 가능
                        .country("KR") // 기본값
                        .currency("KRW") // 기본값
                        .build();
                return stockRepository.save(newStock);
            });

            com.sxxm.stockknock.watchlist.entity.WatchlistId id = 
                    new com.sxxm.stockknock.watchlist.entity.WatchlistId(userId, stock.getSymbol());
            if (!watchlistRepository.existsById(id)) {
                Watchlist watchlist = Watchlist.builder()
                        .userId(userId)
                        .stockSymbol(stock.getSymbol())
                        .user(user)
                        .stock(stock)
                        .build();
                watchlistRepository.save(watchlist);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace(); // 디버깅을 위해 스택 트레이스 출력
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{stockSymbol}")
    public ResponseEntity<Void> removeFromWatchlist(
            @RequestHeader("Authorization") String token,
            @PathVariable String stockSymbol) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            com.sxxm.stockknock.watchlist.entity.WatchlistId id = 
                    new com.sxxm.stockknock.watchlist.entity.WatchlistId(userId, stockSymbol);
            watchlistRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

