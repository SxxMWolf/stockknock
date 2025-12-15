package com.sxxm.stockknock.auth.repository;

import com.sxxm.stockknock.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailAndVerificationCodeAndIsVerifiedFalseAndExpiresAtAfter(
            String email, String verificationCode, LocalDateTime now);
    
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    
    void deleteByEmailAndIsVerifiedTrue(String email);
    
    void deleteByExpiresAtBefore(LocalDateTime now);
}



