package com.npci.notification.dto;

public record NotificationStatusResponse(
        String notificationId, String customerId, String channel, String status, int retryCount) {
}