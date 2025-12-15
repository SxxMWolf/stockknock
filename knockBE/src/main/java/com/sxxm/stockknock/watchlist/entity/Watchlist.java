package com.sxxm.stockknock.watchlist.entity;

import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(WatchlistId.class)
public class Watchlist {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "stock_symbol")
    private String stockSymbol;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

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

