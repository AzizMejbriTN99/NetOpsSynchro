package com.mejbri.pfe.netopssynchro.entity;

public enum NotificationType {
    // ADMIN
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_TOGGLED,

    // CONSULTANT
    TICKET_ASSIGNED,
    TICKET_UPDATED,
    TICKET_RESOLVED,

    // TECHNICIAN
    TASK_ASSIGNED,
    TASK_UPDATED,
    TASK_COMPLETED
}