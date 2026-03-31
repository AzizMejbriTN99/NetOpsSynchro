package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeMapDTO {
    private Long id;
    private String title;
    private String clientName;
    private String clientLocation;
    private DemandeStatus status;
    private DemandePriority priority;
    private double latitude;
    private double longitude;
    private String technicianUsername;
    private Long technicianId;
    private String latestAction;
}