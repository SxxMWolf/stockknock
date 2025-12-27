package com.sxxm.stockknock.news.entity;

/**
 * 시장 브리핑 생성 상태 열거형
 * 
 * 역할:
 * - SUCCESS: 브리핑 생성 성공
 * - FAILED: 브리핑 생성 실패 (GPT 호출 실패 등)
 */
public enum BriefingStatus {
    SUCCESS,  // 성공
    FAILED    // 실패
}

