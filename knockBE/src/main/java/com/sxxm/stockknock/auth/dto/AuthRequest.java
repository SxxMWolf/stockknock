package com.sxxm.stockknock.auth.dto;

/**
 * 인증 요청 DTO
 * 
 * 역할:
 * - 로그인/회원가입 요청 데이터
 * - 이메일, 비밀번호, 사용자명, 닉네임 포함
 */
import lombok.Data;

@Data
public class AuthRequest {
    private String username; // 로그인 시 사용
    private String email;
    private String password;
    private String nickname; // 회원가입 시에만 사용
}

