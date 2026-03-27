package com.mejbri.pfe.netopssynchro.repository.NotificationRepository;

import com.mejbri.pfe.netopssynchro.entity.Notification;
import com.mejbri.pfe.netopssynchro.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetRoleOrderByCreatedAtDesc(Role role);
    long countByTargetRoleAndReadFalse(Role role);
}