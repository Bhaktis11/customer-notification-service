package com.npci.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
        @NotBlank String customerId,
        @NotBlank @Pattern(regexp = "SMS|EMAIL|PUSH", message = "channel must be SMS, EMAIL or PUSH") String channel,
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 100) String idempotencyKey) {
}