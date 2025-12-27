package com.sxxm.stockknock.news.entity;

/**
 * 시장 브리핑 엔티티
 * 
 * 역할:
 * - 오늘의 시장 브리핑 내용 저장
 * - 전역 브리핑 (userId = 0) 또는 사용자별 브리핑
 * - 생성 상태 관리 (SUCCESS, FAILED)
 * - 날짜별로 하루 1회만 생성
 */
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_briefing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketBriefing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private BriefingStatus status = BriefingStatus.SUCCESS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (date == null) {
            date = LocalDate.now();
        }
        if (status == null) {
            status = BriefingStatus.SUCCESS;
        }
    }
}

