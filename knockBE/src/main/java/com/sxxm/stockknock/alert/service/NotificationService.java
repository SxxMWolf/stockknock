package com.sxxm.stockknock.alert.service;

import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.auth.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 알림 발송 서비스 (이메일, SMS, 푸시 알림)
 */
@Service
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * 가격 알림 발송
     */
    public void sendPriceAlert(User user, String message, Stock stock) {
        System.out.println("알림 발송: " + user.getEmail() + " - " + message);
        
        // 이메일 알림
        if (emailEnabled && mailSender != null && fromEmail != null && !fromEmail.isEmpty()) {
            sendEmail(user.getEmail(), "주식 가격 알림", message);
        }

        // TODO: SMS 알림 (Twilio, AWS SNS 등)
        // TODO: 푸시 알림 (FCM, OneSignal 등)
    }

    /**
     * 이메일 발송
     */
    public void sendEmail(String to, String subject, String text) {
        if (mailSender == null) {
            System.out.println("이메일 서비스가 설정되지 않았습니다.");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            System.out.println("이메일 발송 완료: " + to);
        } catch (Exception e) {
            System.err.println("이메일 발송 실패: " + e.getMessage());
        }
    }
}

