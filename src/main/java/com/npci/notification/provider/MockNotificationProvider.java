package com.npci.notification.provider;

import com.npci.notification.entity.Notification;
import com.npci.notification.exception.ProviderException;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockNotificationProvider {

    private final Random random = new Random();

    public void send(Notification notification) {
        int number = random.nextInt(100);
        if (number < 30) {
            throw new ProviderException("Provider timeout");
        }
        // otherwise the message is treated as sent successfully
    }
}