package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class PushNotificationService {

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

    @Value("${firebase.server.key:}")
    private String serverKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendToUser(User user, String title, String body) {
        if (serverKey == null || serverKey.isBlank()) {
            log.debug("FCM server key not configured — skipping push to {}", user.getUsername());
            return;
        }
        String token = user.getFcmToken();
        if (token == null || token.isBlank()) {
            log.debug("No FCM token for user {} — skipping push", user.getUsername());
            return;
        }
        send(token, title, body);
    }

    private void send(String token, String title, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + serverKey);

            Map<String, Object> payload = Map.of(
                    "to", token,
                    "notification", Map.of(
                            "title", title,
                            "body",  body,
                            "sound", "default"
                    ),
                    "priority", "high"
            );

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(FCM_URL, req, String.class);
            log.debug("FCM response [{}]: {}", resp.getStatusCode(), resp.getBody());
        } catch (Exception e) {
            log.warn("Failed to send FCM push: {}", e.getMessage());
        }
    }
}
