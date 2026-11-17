package com.npci.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_customer_idempotency",
                columnNames = {"customer_id", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_notifications_customer_created", columnList = "customer_id, created_at"),
                @Index(name = "idx_notifications_status_retry", columnList = "status, next_retry_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = "N" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
        if (status == null) {
            status = NotificationStatus.QUEUED;
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}