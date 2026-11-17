package com.npci.notification.controller;

import com.npci.notification.dto.CreateNotificationRequest;
import com.npci.notification.dto.NotificationResponse;
import com.npci.notification.dto.NotificationStatusResponse;
import com.npci.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public NotificationStatusResponse getStatus(@PathVariable String id) {
        return service.getStatus(id);
    }
}