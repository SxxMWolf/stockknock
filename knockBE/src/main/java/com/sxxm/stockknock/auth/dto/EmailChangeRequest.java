package com.sxxm.stockknock.auth.dto;

import lombok.Data;

@Data
public class EmailChangeRequest {
    private String newEmail;
    private String verificationCode;
    private String password; // 현재 비밀번호 (보안 확인용)
}



