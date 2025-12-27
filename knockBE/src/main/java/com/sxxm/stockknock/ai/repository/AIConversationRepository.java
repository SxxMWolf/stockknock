package com.sxxm.stockknock.ai.repository;

/**
 * AI 대화 레포지토리
 * 
 * 역할:
 * - AI 대화 엔티티의 데이터베이스 접근
 * - 사용자별 대화 기록 조회 (최신순)
 * - 대화 맥락 유지를 위한 이력 관리
 */
import com.sxxm.stockknock.ai.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    List<AIConversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}

