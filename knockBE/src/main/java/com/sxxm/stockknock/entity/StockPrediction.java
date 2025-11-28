package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_predictions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    private PredictionType predictionType; // 단기, 장기

    private BigDecimal predictedPrice;

    private LocalDate targetDate;

    @Column(columnDefinition = "TEXT")
    private String analysis; // 예측 근거

    private BigDecimal confidence; // 신뢰도 (0-100)

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum PredictionType {
        SHORT_TERM, LONG_TERM
    }
}

