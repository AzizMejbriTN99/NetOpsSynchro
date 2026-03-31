package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DemandeActionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeActionDTO {
    private Long id;
    private DemandeActionStatus status;
    private String note;
    private String performedBy;
    private LocalDateTime performedAt;
}
