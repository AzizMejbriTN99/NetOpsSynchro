package com.mejbri.pfe.netopssynchro.controller;


import com.mejbri.pfe.netopssynchro.entity.Notification;
import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private Role extractRole(Authentication auth) {
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        return Role.valueOf(authority.replace("ROLE_", ""));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getAll(Authentication auth) {
        return ResponseEntity.ok(notificationService.getForRole(extractRole(auth)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(extractRole(auth))));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        notificationService.markAllRead(extractRole(auth));
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markOneRead(@PathVariable Long id) {
        notificationService.markOneRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }
}

