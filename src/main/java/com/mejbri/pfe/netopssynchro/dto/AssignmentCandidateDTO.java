package com.mejbri.pfe.netopssynchro.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentCandidateDTO {
    private Long   technicianId;
    private String technicianUsername;
    private Long   demandeId;
    private String demandeTitle;
    private double directDistanceKm;
    private double estimatedMinutes;
    private boolean needsDepotStop;
    private double  depotDistanceKm;
    private double  totalDistanceKm;
    private String  reasoning;
}
