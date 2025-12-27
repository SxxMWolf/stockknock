package com.sxxm.stockknock.auth.service;

/**
 * 이메일 인증 서비스
 * 
 * 역할:
 * - 이메일 변경 시 인증 코드 발송
 * - 인증 코드 생성 및 검증
 * - 인증 코드 만료 시간 관리 (10분)
 * - 만료된 인증 코드 자동 삭제 (스케줄러)
 */
import com.sxxm.stockknock.alert.service.NotificationService;
import com.sxxm.stockknock.auth.entity.EmailVerification;
import com.sxxm.stockknock.auth.repository.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 인증 코드 생성 및 이메일 전송
     */
    public void sendVerificationCode(String email) {
        // 6자리 랜덤 코드 생성
        String code = generateVerificationCode();
        
        // 기존 미인증 코드가 있으면 삭제
        Optional<EmailVerification> existing = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email);
        if (existing.isPresent() && !existing.get().getIsVerified()) {
            emailVerificationRepository.delete(existing.get());
        }
        
        // 새 인증 코드 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .isVerified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        
        emailVerificationRepository.save(verification);
        
        // 이메일 전송
        String subject = "StocKKnock 이메일 인증 코드";
        String message = String.format(
                "안녕하세요.\n\n" +
                "StocKKnock 이메일 인증 코드입니다.\n\n" +
                "인증 코드: %s\n\n" +
                "이 코드는 10분간 유효합니다.\n" +
                "본인이 요청한 것이 아니라면 무시하세요.\n\n" +
                "감사합니다.",
                code
        );
        
        notificationService.sendEmail(email, subject, message);
    }

    /**
     * 인증 코드 검증
     */
    public boolean verifyCode(String email, String code) {
        Optional<EmailVerification> verification = emailVerificationRepository
                .findByEmailAndVerificationCodeAndIsVerifiedFalseAndExpiresAtAfter(
                        email, code, LocalDateTime.now());
        
        if (verification.isPresent()) {
            EmailVerification v = verification.get();
            v.setIsVerified(true);
            emailVerificationRepository.save(v);
            return true;
        }
        
        return false;
    }

    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String email) {
        Optional<EmailVerification> verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email);
        
        return verification.isPresent() && verification.get().getIsVerified();
    }

    /**
     * 6자리 랜덤 인증 코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }

    /**
     * 만료된 인증 코드 정리 (매일 자정 실행)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupExpiredCodes() {
        emailVerificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}



