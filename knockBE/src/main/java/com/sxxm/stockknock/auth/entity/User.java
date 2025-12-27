/**
 * 사용자 계정 정보 저장. 포트폴리오, 관심 종목, 가격 알림과의 연관관계.
 */
package com.sxxm.stockknock.auth.entity;

import com.sxxm.stockknock.portfolio.entity.Portfolio;
import com.sxxm.stockknock.alert.entity.PriceAlert;
import com.sxxm.stockknock.watchlist.entity.Watchlist;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nickname;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Portfolio> portfolios;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Watchlist> watchlists;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PriceAlert> priceAlerts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<com.sxxm.stockknock.ai.entity.AIConversation> aiConversations;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

