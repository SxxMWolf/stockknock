package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
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
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private AlertType alertType; // 목표가, 손절가, 변동률

    private BigDecimal targetPrice;

    private BigDecimal percentageChange; // 변동률 기준

    private Boolean isTriggered;

    private LocalDateTime triggeredAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isTriggered = false;
    }

    public enum AlertType {
        TARGET_PRICE, STOP_LOSS, PERCENTAGE_CHANGE
    }
}

