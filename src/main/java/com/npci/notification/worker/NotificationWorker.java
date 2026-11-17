package com.npci.notification.worker;

import com.npci.notification.entity.Notification;
import com.npci.notification.entity.NotificationStatus;
import com.npci.notification.exception.ProviderException;
import com.npci.notification.provider.MockNotificationProvider;
import com.npci.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Component
public class NotificationWorker {

    private final NotificationRepository repository;
    private final MockNotificationProvider provider;
    private final Random random = new Random();

    @Value("${notification.retry.max}")
    private int maxRetries;

    @Value("${notification.retry.base-delay-ms}")
    private long baseDelayMs;

    public NotificationWorker(NotificationRepository repository, MockNotificationProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    @Scheduled(fixedDelay = 2000)
    public void processNotifications() {
        // 1. new notifications
        List<Notification> queued = repository.findTop20ByStatusOrderByCreatedAtAsc(NotificationStatus.QUEUED);
        for (Notification notification : queued) {
            process(notification);
        }

        // 2. notifications whose retry time has come
        List<Notification> retries = repository.findTop20ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                NotificationStatus.RETRYING, Instant.now());
        for (Notification notification : retries) {
            process(notification);
        }
    }

    private void process(Notification notification) {
        notification.setStatus(NotificationStatus.PROCESSING);
        repository.save(notification);

        try {
            provider.send(notification);
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setNextRetryAt(null);
            notification.setFailureReason(null);
        } catch (ProviderException e) {
            handleFailure(notification, e.getMessage());
        }
        repository.save(notification);
    }

    private void handleFailure(Notification notification, String reason) {
        notification.setFailureReason(reason);

        if (notification.getRetryCount() < maxRetries) {
            int newCount = notification.getRetryCount() + 1;
            notification.setRetryCount(newCount);
            notification.setStatus(NotificationStatus.RETRYING);
            notification.setNextRetryAt(Instant.now().plusMillis(calculateDelay(newCount)));
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setNextRetryAt(null);
        }
    }

    // 2s, 4s, 8s ... plus up to 1 second of random jitter
    private long calculateDelay(int retryCount) {
        long delay = baseDelayMs;
        for (int i = 1; i < retryCount; i++) {
            delay = delay * 2;
        }
        long jitter = random.nextInt(1000);
        return delay + jitter;
    }
}