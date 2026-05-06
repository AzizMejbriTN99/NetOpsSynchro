package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.entity.Notification;
import com.mejbri.pfe.netopssynchro.entity.NotificationType;
import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.repository.NotificationRepository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private Role resolveRole(NotificationType type) {
        return switch (type) {
            case USER_CREATED, USER_UPDATED,
                    USER_DELETED, USER_TOGGLED -> Role.ADMIN;
            case TICKET_ASSIGNED, TICKET_UPDATED,
                    TICKET_RESOLVED,
                    SERVER_ERROR_DETECTED -> Role.CONSULTANT;
            case TASK_ASSIGNED, TASK_UPDATED,
                    TASK_COMPLETED, AI_ASSIGNMENT -> Role.TECHNICIAN;
        };
    }

    private String buildMessage(NotificationType type, String target) {
        return switch (type) {
            case USER_CREATED -> "New account created: " + target;
            case USER_UPDATED -> "Account updated: " + target;
            case USER_DELETED -> "Account deleted: " + target;
            case USER_TOGGLED -> "Account status changed: " + target;
            case TICKET_ASSIGNED -> "Ticket assigned: " + target;
            case TICKET_UPDATED -> "Ticket updated: " + target;
            case TICKET_RESOLVED -> "Ticket resolved: " + target;
            case TASK_ASSIGNED -> "New task assigned: " + target;
            case TASK_UPDATED -> "Task updated: " + target;
            case TASK_COMPLETED -> "Task completed: " + target;
            case AI_ASSIGNMENT -> "AI assigned you a new task: " + target;
            case SERVER_ERROR_DETECTED -> "Server error detected: " + target;
        };
    }

    public void push(NotificationType type, String targetUsername) {
        save(type, targetUsername, buildMessage(type, targetUsername));
    }

    public void pushCustom(NotificationType type, String targetUsername, String message) {
        save(type, targetUsername, message);
    }

    private void save(NotificationType type, String targetUsername, String message) {
        notificationRepository.save(Notification.builder()
                .type(type)
                .targetRole(resolveRole(type))
                .message(message)
                .targetUsername(targetUsername)
                .read(false)
                .build());
    }

    public List<Notification> getForRole(Role role) {
        return notificationRepository.findByTargetRoleOrderByCreatedAtDesc(role);
    }

    public long getUnreadCount(Role role) {
        return notificationRepository.countByTargetRoleAndReadFalse(role);
    }

    public void markAllRead(Role role) {
        List<Notification> list = notificationRepository.findByTargetRoleOrderByCreatedAtDesc(role);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }

    public void markOneRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
