package com.phegon.FoodApp.email_notification.services;

import com.phegon.FoodApp.email_notification.dtos.NotificationDTO;
import com.phegon.FoodApp.email_notification.entity.Notification;
import com.phegon.FoodApp.email_notification.repository.NotificationRepository;
import com.phegon.FoodApp.enums.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;
    @Value("${spring.mail.username:}")
    private String fromEmail;


    @Override
    @Async
    public void sendEmail(NotificationDTO notificationDTO) {
        log.info("=== Inside sendEmail() ===");

        // 🔍 DEBUG ENV (CỰC KỲ QUAN TRỌNG CHO PRODUCTION)
        log.info("MAIL_HOST = {}", System.getenv("MAIL_HOST"));
        log.info("MAIL_USERNAME = {}", System.getenv("MAIL_USERNAME"));
        log.info("spring.mail.username (fromEmail) = {}", fromEmail);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            // ✅ FIX: fallback FROM nếu production bị rỗng
            String sender = (fromEmail == null || fromEmail.isBlank())
                    ? "no-reply@foodapp.com"
                    : fromEmail;

            helper.setFrom(sender);
            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());
            helper.setText(notificationDTO.getBody(), notificationDTO.isHtml());

            log.info("📤 Sending email to {}", notificationDTO.getRecipient());
            javaMailSender.send(mimeMessage);
            log.info("✅ Email sent successfully");

            // 💾 SAVE TO DATABASE
            Notification notificationToSave = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .body(notificationDTO.getBody())
                    .type(NotificationType.EMAIL)
                    .isHtml(notificationDTO.isHtml())
                    .build();

            notificationRepository.save(notificationToSave);
            log.info("💾 Saved email notification to DB");

        } catch (Exception e) {
            log.error("❌ Failed to send email to {}",
                    notificationDTO.getRecipient(), e);
            throw new RuntimeException(e);
        }
    }
}








