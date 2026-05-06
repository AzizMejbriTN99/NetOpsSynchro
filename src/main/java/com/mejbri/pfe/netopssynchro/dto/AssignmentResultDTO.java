package com.mejbri.pfe.netopssynchro.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResultDTO {
    private List<AssignmentCandidateDTO> assignments;
    private String  summary;
    private double  totalOptimalTimeMinutes;
    private boolean autoApplied;
}
