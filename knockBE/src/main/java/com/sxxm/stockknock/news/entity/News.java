package com.sxxm.stockknock.news.entity;

/**
 * 뉴스 엔티티
 * 
 * 역할:
 * - 뉴스 기사 정보 저장 (제목, 내용, 출처, URL)
 * - 뉴스 발행 시간 기록
 * - 뉴스 분석 결과와의 연관관계
 */
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "news")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String source; // 뉴스 출처

    @Column(columnDefinition = "TEXT")
    private String url;

    private LocalDateTime publishedAt;

    @OneToOne(mappedBy = "news", cascade = CascadeType.ALL)
    private NewsAnalysis analysis;

    @OneToMany(mappedBy = "news", cascade = CascadeType.ALL)
    private List<com.sxxm.stockknock.news.entity.NewsStockRelation> stockRelations;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

