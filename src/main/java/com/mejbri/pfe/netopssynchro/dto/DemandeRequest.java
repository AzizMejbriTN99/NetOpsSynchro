package com.mejbri.pfe.netopssynchro.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mejbri.pfe.netopssynchro.entity.DemandePriority;
import com.mejbri.pfe.netopssynchro.entity.DemandeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class DemandeRequest {
    private String title;
    private String description;
    private DemandePriority priority;
    private DemandeStatus status;
    private String clientName;
    private String clientContact;
    private String clientLocation;
    private Long   technicianId;
    private Double latitude;
    private Double longitude;
}
