package com.mejbri.pfe.netopssynchro.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.mejbri.pfe.netopssynchro.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PushNotificationService {

    private static final String FCM_URL_TEMPLATE =
            "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendToUser(User user, String title, String body) {

        String token = user.getFcmToken();
        if (token == null || token.isBlank()) {
            log.debug("No FCM token for user {} — skipping push", user.getUsername());
            return;
        }

        try {
            send(token, title, body);
        } catch (Exception e) {
            log.warn("Failed to send FCM push: {}", e.getMessage(), e);
        }
    }

    private void send(String token, String title, String body) throws IOException {

        String accessToken = getAccessToken();

        String url = String.format(FCM_URL_TEMPLATE, projectId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> message = Map.of(
                "token", token,
                "notification", Map.of(
                        "title", title,
                        "body", body
                ),
                "android", Map.of(
                        "priority", "HIGH"
                )
        );

        Map<String, Object> payload = Map.of("message", message);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        log.debug("FCM v1 response [{}]: {}", response.getStatusCode(), response.getBody());
    }

    private String getAccessToken() throws IOException {

        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ClassPathResource(serviceAccountPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));

        credentials.refreshIfExpired();

        return credentials.getAccessToken().getTokenValue();
    }
}