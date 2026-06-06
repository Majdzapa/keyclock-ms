package com.incert.certmanager.scheduler;

import com.incert.certmanager.domain.Certificate;
import com.incert.certmanager.notification.NotificationService;
import com.incert.certmanager.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateExpiryScheduler {

    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService;

    @Value("${app.scheduler.expiry-threshold-hours:72}")
    private int expiryThresholdHours;


    @Scheduled(cron = "${app.scheduler.cron:0 0 * * * *}")
    public void checkExpiringCertificates() {
        int thresholdHours = expiryThresholdHours;
        LocalDateTime threshold = LocalDateTime.now().plusHours(thresholdHours);
        List<Certificate> expiring = certificateRepository.findAllExpiringBefore(threshold);

        if (expiring.isEmpty()) {
            return;
        }
        notificationService.sendBatchExpiryAlert(expiring);
    }
}
