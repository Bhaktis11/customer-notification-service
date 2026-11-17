package com.npci.notification;

import com.npci.notification.dto.CreateNotificationRequest;
import com.npci.notification.dto.NotificationResponse;
import com.npci.notification.exception.IdempotencyConflictException;
import com.npci.notification.repository.NotificationRepository;
import com.npci.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificationIdempotencyIntegrationTest {

	@Autowired
	private NotificationService service;

	@Autowired
	private NotificationRepository repository;

	@Test
	void sameRequestTwice_returnsSameIdAndStoresOneRow() {
		String key = "IT-" + System.currentTimeMillis();
		CreateNotificationRequest request = new CreateNotificationRequest("CUST-IT", "SMS", "Hello", key);

		NotificationResponse first = service.create(request);
		NotificationResponse second = service.create(request);

		assertEquals(first.notificationId(), second.notificationId());
		assertTrue(repository.findByCustomerIdAndIdempotencyKey("CUST-IT", key).isPresent());
	}

	@Test
	void sameKeyDifferentMessage_throwsConflict() {
		String key = "IT2-" + System.currentTimeMillis();
		service.create(new CreateNotificationRequest("CUST-IT", "SMS", "Hello", key));

		try {
			service.create(new CreateNotificationRequest("CUST-IT", "SMS", "Changed", key));
			fail("Expected IdempotencyConflictException");
		} catch (IdempotencyConflictException e) {
			// expected
		}
	}
}