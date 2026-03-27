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

    // resolve which role owns which notification type
    private Role resolveRole(NotificationType type) {
        return switch (type) {
            case USER_CREATED, USER_UPDATED,
                    USER_DELETED, USER_TOGGLED      -> Role.ADMIN;
            case TICKET_ASSIGNED, TICKET_UPDATED,
                    TICKET_RESOLVED                 -> Role.CONSULTANT;
            case TASK_ASSIGNED, TASK_UPDATED,
                    TASK_COMPLETED                  -> Role.TECHNICIAN;
        };
    }

    private String buildMessage(NotificationType type, String target) {
        return switch (type) {
            case USER_CREATED    -> "New account created: " + target;
            case USER_UPDATED    -> "Account updated: " + target;
            case USER_DELETED    -> "Account deleted: " + target;
            case USER_TOGGLED    -> "Account status changed: " + target;
            case TICKET_ASSIGNED -> "Ticket assigned to you: " + target;
            case TICKET_UPDATED  -> "Ticket updated: " + target;
            case TICKET_RESOLVED -> "Ticket resolved: " + target;
            case TASK_ASSIGNED   -> "New task assigned: " + target;
            case TASK_UPDATED    -> "Task updated: " + target;
            case TASK_COMPLETED  -> "Task marked complete: " + target;
        };
    }

    public void push(NotificationType type, String targetUsername) {
        Notification n = Notification.builder()
                .type(type)
                .targetRole(resolveRole(type))
                .message(buildMessage(type, targetUsername))
                .targetUsername(targetUsername)
                .read(false)
                .build();
        notificationRepository.save(n);
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
