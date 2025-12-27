/**
 * 포트폴리오 AI 분석 결과 저장. 사용자당 1개만 유지, 종목 변경 시 재분석.
 */
package com.sxxm.stockknock.portfolio.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_analysis", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "portfolio_hash", nullable = false, length = 64)
    private String portfolioHash; // SHA-256 해시

    @Column(name = "analysis_content", columnDefinition = "TEXT")
    private String analysisContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.SUCCESS;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = AnalysisStatus.SUCCESS;
        }
    }

    public enum AnalysisStatus {
        SUCCESS,
        FAILED
    }
}

