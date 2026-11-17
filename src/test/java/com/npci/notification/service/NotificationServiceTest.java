package com.npci.notification.service;

import com.npci.notification.dto.CreateNotificationRequest;
import com.npci.notification.dto.NotificationResponse;
import com.npci.notification.entity.Notification;
import com.npci.notification.entity.NotificationStatus;
import com.npci.notification.exception.IdempotencyConflictException;
import com.npci.notification.exception.NotificationNotFoundException;
import com.npci.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    private CreateNotificationRequest request() {
        return new CreateNotificationRequest("CUST1", "SMS", "Payment Successful", "KEY1");
    }

    private Notification existing(String message) {
        Notification n = new Notification();
        n.setId("N1");
        n.setCustomerId("CUST1");
        n.setChannel("SMS");
        n.setMessage(message);
        n.setStatus(NotificationStatus.QUEUED);
        n.setIdempotencyKey("KEY1");
        return n;
    }

    @Test
    void create_newRequest_savesAsQueued() {
        when(repository.findByCustomerIdAndIdempotencyKey("CUST1", "KEY1")).thenReturn(Optional.empty());
        when(repository.save(any(Notification.class))).thenReturn(existing("Payment Successful"));

        NotificationResponse response = service.create(request());

        assertEquals("N1", response.notificationId());
        assertEquals("QUEUED", response.status());
        verify(repository, times(1)).save(any(Notification.class));
    }

    @Test
    void create_sameKeySamePayload_returnsOriginalAndDoesNotSave() {
        when(repository.findByCustomerIdAndIdempotencyKey("CUST1", "KEY1"))
                .thenReturn(Optional.of(existing("Payment Successful")));

        NotificationResponse response = service.create(request());

        assertEquals("N1", response.notificationId());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void create_sameKeyDifferentPayload_throwsConflict() {
        when(repository.findByCustomerIdAndIdempotencyKey("CUST1", "KEY1"))
                .thenReturn(Optional.of(existing("A different message")));

        try {
            service.create(request());
            fail("Expected IdempotencyConflictException");
        } catch (IdempotencyConflictException e) {
            verify(repository, never()).save(any(Notification.class));
        }
    }

    @Test
    void getStatus_unknownId_throwsNotFound() {
        when(repository.findById("X1")).thenReturn(Optional.empty());

        try {
            service.getStatus("X1");
            fail("Expected NotificationNotFoundException");
        } catch (NotificationNotFoundException e) {
            assertTrue(e.getMessage().contains("X1"));
        }
    }
}