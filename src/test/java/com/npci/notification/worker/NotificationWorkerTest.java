package com.npci.notification.worker;

import com.npci.notification.entity.Notification;
import com.npci.notification.entity.NotificationStatus;
import com.npci.notification.exception.ProviderException;
import com.npci.notification.provider.MockNotificationProvider;
import com.npci.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationWorkerTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private MockNotificationProvider provider;

    private NotificationWorker worker;

    @BeforeEach
    void setUp() {
        worker = new NotificationWorker(repository, provider);
        ReflectionTestUtils.setField(worker, "maxRetries", 3);
        ReflectionTestUtils.setField(worker, "baseDelayMs", 2000L);
    }

    private Notification queued(int retryCount) {
        Notification n = new Notification();
        n.setId("N1");
        n.setStatus(NotificationStatus.QUEUED);
        n.setRetryCount(retryCount);
        return n;
    }

    private void givenQueued(Notification n) {
        when(repository.findTop20ByStatusOrderByCreatedAtAsc(NotificationStatus.QUEUED))
                .thenReturn(List.of(n));
    }

    @Test
    void providerSuccess_marksDelivered() {
        Notification n = queued(0);
        givenQueued(n);

        worker.processNotifications();

        assertEquals(NotificationStatus.DELIVERED, n.getStatus());
        assertNull(n.getNextRetryAt());
    }

    @Test
    void providerFailure_firstTime_marksRetryingWithNextRetryTime() {
        Notification n = queued(0);
        givenQueued(n);
        doThrow(new ProviderException("Provider timeout")).when(provider).send(any(Notification.class));

        worker.processNotifications();

        assertEquals(NotificationStatus.RETRYING, n.getStatus());
        assertEquals(1, n.getRetryCount());
        assertNotNull(n.getNextRetryAt());
        assertEquals("Provider timeout", n.getFailureReason());
    }

    @Test
    void providerFailure_afterMaxRetries_marksFailed() {
        Notification n = queued(3);
        givenQueued(n);
        doThrow(new ProviderException("Provider timeout")).when(provider).send(any(Notification.class));

        worker.processNotifications();

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals(3, n.getRetryCount());
        assertNull(n.getNextRetryAt());
    }

    @Test
    void backoffDelayDoubles_secondRetryWaitsAtLeastFourSeconds() {
        Notification n = queued(1);
        givenQueued(n);
        doThrow(new ProviderException("Provider timeout")).when(provider).send(any(Notification.class));

        Instant before = Instant.now();
        worker.processNotifications();

        assertEquals(2, n.getRetryCount());
        assertFalse(n.getNextRetryAt().isBefore(before.plusMillis(4000)));
    }
}