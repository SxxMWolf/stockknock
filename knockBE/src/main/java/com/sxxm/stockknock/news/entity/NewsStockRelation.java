package com.sxxm.stockknock.news.entity;

import com.sxxm.stockknock.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_stock_relation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(NewsStockRelationId.class)
public class NewsStockRelation {
    @Id
    @Column(name = "news_id")
    private Long newsId;

    @Id
    @Column(name = "stock_symbol")
    private String stockSymbol;

    @ManyToOne
    @JoinColumn(name = "news_id", insertable = false, updatable = false)
    private News news;

    @ManyToOne
    @JoinColumn(name = "stock_symbol", insertable = false, updatable = false, referencedColumnName = "symbol")
    private Stock stock;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

