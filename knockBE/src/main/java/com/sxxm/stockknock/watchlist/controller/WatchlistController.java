package com.sxxm.stockknock.watchlist.controller;

import com.sxxm.stockknock.stock.dto.StockDto;
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
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<StockDto>> getWatchlist(@RequestHeader("Authorization") String token) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            List<Watchlist> watchlists = watchlistRepository.findByUserId(userId);
            List<StockDto> stocks = watchlists.stream()
                    .map(w -> stockService.convertToDto(w.getStock()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(stocks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{stockSymbol}")
    public ResponseEntity<Void> addToWatchlist(
            @RequestHeader("Authorization") String token,
            @PathVariable String stockSymbol) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            var user = userService.getUserEntityById(userId);
            var stock = stockService.getStockEntityBySymbol(stockSymbol);

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

