package com.sxxm.stockknock.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private static final int MIN_SECRET_LENGTH = 64; // 최소 64 bytes

    /**
     * JWT Secret 길이 검증 (보안 강화)
     * 최소 64 bytes 이상이어야 함
     * 프로덕션 환경에서만 엄격하게 검증
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET이 설정되지 않았습니다. 환경 변수를 확인하세요.");
        }

        int secretLength = secret.getBytes(StandardCharsets.UTF_8).length;
        
        // 프로덕션 환경 체크 (환경 변수 또는 프로파일로 확인)
        String activeProfile = System.getProperty("spring.profiles.active", "");
        boolean isProduction = activeProfile.contains("prod") || activeProfile.contains("production");
        
        if (secretLength < MIN_SECRET_LENGTH) {
            if (isProduction) {
                // 프로덕션 환경에서는 엄격하게 검증
                throw new IllegalStateException(
                    String.format(
                        "보안 위험: 프로덕션 환경에서는 JWT_SECRET이 최소 %d bytes 이상이어야 합니다. 현재 길이: %d bytes. " +
                        "다음 명령어로 안전한 키를 생성하세요: " +
                        "openssl rand -base64 64 | tr -d '\n'",
                        MIN_SECRET_LENGTH, secretLength
                    )
                );
            } else {
                // 개발 환경에서는 경고만 출력
                System.err.println(
                    String.format(
                        "⚠️  경고: JWT_SECRET이 %d bytes 미만입니다 (현재: %d bytes). " +
                        "프로덕션 환경에서는 최소 %d bytes 이상이어야 합니다. " +
                        "생성 명령어: openssl rand -base64 64 | tr -d '\n'",
                        MIN_SECRET_LENGTH, secretLength, MIN_SECRET_LENGTH
                    )
                );
            }
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

