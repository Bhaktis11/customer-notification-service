package com.npci.notification.service;

import com.npci.notification.dto.CreateNotificationRequest;
import com.npci.notification.dto.NotificationResponse;
import com.npci.notification.dto.NotificationStatusResponse;
import com.npci.notification.entity.Notification;
import com.npci.notification.entity.NotificationStatus;
import com.npci.notification.exception.IdempotencyConflictException;
import com.npci.notification.exception.NotificationNotFoundException;
import com.npci.notification.repository.NotificationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public NotificationResponse create(CreateNotificationRequest request) {

        String key = request.idempotencyKey();
        if (key != null && key.isBlank()) {
            key = null;
        }

        // Step 2: check if this request was already received
        if (key != null) {
            Optional<Notification> existing =
                    repository.findByCustomerIdAndIdempotencyKey(request.customerId(), key);
            if (existing.isPresent()) {
                return handleExisting(existing.get(), request);
            }
        }

        // Step 3: save a new notification as QUEUED
        Notification notification = new Notification();
        notification.setCustomerId(request.customerId());
        notification.setChannel(request.channel());
        notification.setMessage(request.message());
        notification.setIdempotencyKey(key);
        notification.setStatus(NotificationStatus.QUEUED);

        try {
            Notification saved = repository.save(notification);
            return new NotificationResponse(saved.getId(), saved.getStatus().name());
        } catch (DataIntegrityViolationException e) {
            // Step 4: two identical requests arrived at the same time
            Optional<Notification> existing =
                    repository.findByCustomerIdAndIdempotencyKey(request.customerId(), key);
            if (existing.isPresent()) {
                return handleExisting(existing.get(), request);
            }
            throw e;
        }
    }

    public NotificationStatusResponse getStatus(String id) {
        Optional<Notification> found = repository.findById(id);
        if (found.isEmpty()) {
            throw new NotificationNotFoundException(id);
        }
        Notification n = found.get();
        return new NotificationStatusResponse(
                n.getId(), n.getCustomerId(), n.getChannel(), n.getStatus().name(), n.getRetryCount());
    }

    // Same key + same payload -> return the original. Same key + different payload -> conflict.
    private NotificationResponse handleExisting(Notification existing, CreateNotificationRequest request) {
        boolean samePayload = existing.getChannel().equals(request.channel())
                && existing.getMessage().equals(request.message());

        if (samePayload) {
            return new NotificationResponse(existing.getId(), existing.getStatus().name());
        }
        throw new IdempotencyConflictException(
                "Idempotency key already used with a different request payload");
    }
}