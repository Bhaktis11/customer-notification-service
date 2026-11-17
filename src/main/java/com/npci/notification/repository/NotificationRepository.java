package com.npci.notification.repository;

import com.npci.notification.entity.Notification;
import com.npci.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByCustomerIdAndIdempotencyKey(String customerId, String idempotencyKey);
    List<Notification> findTop20ByStatusOrderByCreatedAtAsc(NotificationStatus status);
    List<Notification> findTop20ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            NotificationStatus status, Instant now);

}