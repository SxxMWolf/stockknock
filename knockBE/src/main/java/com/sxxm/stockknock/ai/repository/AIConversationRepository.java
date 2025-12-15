package com.sxxm.stockknock.ai.repository;

import com.sxxm.stockknock.ai.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    List<AIConversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}

