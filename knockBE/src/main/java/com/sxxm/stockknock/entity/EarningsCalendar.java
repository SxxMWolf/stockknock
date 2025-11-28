package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "earnings_calendar")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EarningsCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    private LocalDate earningsDate;

    private String quarter; // Q1, Q2, Q3, Q4

    private String fiscalYear;

    @Column(columnDefinition = "TEXT")
    private String earningsContent; // 실적 공시 내용

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis; // AI 해석

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

