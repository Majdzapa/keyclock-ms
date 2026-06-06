package com.incert.certmanager.notification;

import com.incert.certmanager.domain.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${app.notification.admin-email:admin@certmanager.local}")
    private String adminEmail;

    @Value("${app.notification.from:noreply@certmanager.local}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void sendBatchExpiryAlert(List<Certificate> certificates) {
        if (certificates.isEmpty()) {
            return;
        }
        certificates.forEach(this::sendExpiryAlert);
    }


    public void sendExpiryAlert(Certificate certificate) {
        String subject = String.format("[CertManager] Certificate Expiry Alert: %s",
                                       certificate.getFilename());
        String body    = buildSingleAlertBody(certificate);
        sendEmail(adminEmail, subject, body);
    }



    // ─────────────────────────── Email Sending ────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to '{}' — {}", to, e.getMessage(), e);
        }
    }

    // ─────────────────────────── Plain-Text Templates ─────────────────────

    private String buildSingleAlertBody(Certificate cert) {
        String expiry = cert.getExpirationDate() != null
                        ? cert.getExpirationDate().format(DATE_FMT) : "Unknown";
        return """
            [CertManager] Certificate Expiry Alert
            =======================================

            The following certificate is expiring soon and requires immediate attention:

              Filename   : %s
              Owner      : %s
              Issuer     : %s
              Expires On : %s
              Group      : %s
              Uploaded By: %s

            Please renew this certificate before it expires.

            --
            This is an automated alert from CertManager.
            """.formatted(
                cert.getFilename(),
                cert.getOwner()           != null ? cert.getOwner()           : "N/A",
                cert.getIssuer()          != null ? cert.getIssuer()          : "N/A",
                expiry,
                cert.getUploadedByGroup() != null ? cert.getUploadedByGroup() : "N/A",
                cert.getUploadedBy()
            );
    }

}
