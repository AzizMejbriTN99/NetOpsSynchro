package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DemandeDTO {
    private Long id;
    private String title;
    private String description;
    private DemandePriority priority;
    private DemandeStatus status;
    private String clientName;
    private String clientContact;
    private String clientLocation;
    private Long technicianId;
    private String technicianUsername;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}