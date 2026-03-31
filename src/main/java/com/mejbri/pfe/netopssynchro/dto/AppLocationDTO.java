package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.LocationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppLocationDTO {
    private Long id;
    private String name;
    private String description;
    private City city;
    private LocationType type;
    private double latitude;
    private double longitude;
    private boolean active;
}