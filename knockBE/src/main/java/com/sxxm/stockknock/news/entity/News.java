package com.sxxm.stockknock.news.entity;

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

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String source; // 뉴스 출처

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

