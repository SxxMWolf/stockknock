package com.sxxm.stockknock.auth.entity;

/**
 * 이메일 인증 엔티티
 * 
 * 역할:
 * - 이메일 변경 시 인증 코드 저장
 * - 인증 코드 만료 시간 관리
 * - 이메일과 인증 코드 인덱스로 빠른 조회
 */
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification", indexes = {
        @Index(name = "idx_email_verification_email", columnList = "email"),
        @Index(name = "idx_email_verification_code", columnList = "verification_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "verification_code", nullable = false, length = 6)
    private String verificationCode;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(10); // 10분 유효
        }
    }
}



