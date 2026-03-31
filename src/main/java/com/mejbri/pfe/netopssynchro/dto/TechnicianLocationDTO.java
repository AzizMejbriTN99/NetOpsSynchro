package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.City;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianLocationDTO {
    private Long technicianId;
    private String username;
    private double latitude;
    private double longitude;
    private City city;
    private LocalDateTime recordedAt;
}
