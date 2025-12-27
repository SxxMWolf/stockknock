/**
 * 주가 알림 설정 및 관리 API. 조회, 생성, 수정, 삭제.
 */
package com.sxxm.stockknock.alert.controller;

import com.sxxm.stockknock.alert.entity.PriceAlert;
import com.sxxm.stockknock.alert.repository.PriceAlertRepository;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.stock.service.StockService;
import com.sxxm.stockknock.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "http://localhost:3000")
public class PriceAlertController {

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private StockService stockService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getAlerts(@RequestHeader("Authorization") String token) {
        try {
            if (token == null || token.length() < 7) {
                System.err.println("Invalid token format: " + token);
                return ResponseEntity.status(401).build();
            }
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            List<PriceAlert> alerts = priceAlertRepository.findByUserId(userId);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            System.err.println("알림 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<PriceAlert> createAlert(
            @RequestHeader("Authorization") String token,
            @RequestParam String stockSymbol,
            @RequestParam String alertType,
            @RequestParam(required = false) BigDecimal targetPrice,
            @RequestParam(required = false) BigDecimal percentageChange) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            var user = userService.getUserEntityById(userId);
            var stock = stockService.getStockEntityBySymbol(stockSymbol);

            PriceAlert alert = PriceAlert.builder()
                    .user(user)
                    .stock(stock)
                    .alertType(alertType) // "TARGET", "STOP_LOSS", "PERCENT"
                    .targetPrice(targetPrice)
                    .percentChange(percentageChange)
                    .build();

            alert = priceAlertRepository.save(alert);
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId) {
        try {
            priceAlertRepository.deleteById(alertId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

