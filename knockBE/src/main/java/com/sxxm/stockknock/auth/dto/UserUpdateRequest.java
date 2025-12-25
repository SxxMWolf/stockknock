package com.sxxm.stockknock.auth.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username; // 아이디 변경
    private String nickname; // 닉네임 변경
    private String currentPassword; // 비밀번호 변경 시 현재 비밀번호
    private String newPassword; // 비밀번호 변경 시 새 비밀번호
}

