package com.sxxm.stockknock.auth.dto;

/**
 * 인증 응답 DTO
 * 
 * 역할:
 * - 로그인 성공 시 반환되는 데이터
 * - JWT 토큰 및 사용자 정보 포함
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String username;
    private String email;
    private String name;
    private Long userId;
}

