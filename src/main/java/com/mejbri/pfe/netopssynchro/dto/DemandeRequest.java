package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import lombok.Data;

@Data
public class DemandeRequest {
    private String title;
    private String description;
    private DemandePriority priority;
    private DemandeStatus status;
    private String clientName;
    private String clientContact;
    private String clientLocation;
    private Long technicianId;
}
