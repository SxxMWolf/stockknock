package com.sxxm.stockknock.alert.entity;

import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_symbol", nullable = false, referencedColumnName = "symbol")
    private Stock stock;

    @Column(name = "alert_type", length = 20, nullable = false)
    private String alertType; // 'TARGET', 'STOP_LOSS', 'PERCENT'

    @Column(name = "target_price", precision = 18, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "percent_change", precision = 5, scale = 2)
    private BigDecimal percentChange; // 변동률 기준

    @Builder.Default
    @Column(nullable = false)
    private Boolean triggered = false;

    private LocalDateTime triggeredAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (triggered == null) {
            triggered = false;
        }
    }
}

