package com.sxxm.stockknock.auth.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username; // 로그인 시 사용
    private String email;
    private String password;
    private String nickname; // 회원가입 시에만 사용
}

