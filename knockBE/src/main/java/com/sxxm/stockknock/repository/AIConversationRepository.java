package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    List<AIConversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}

