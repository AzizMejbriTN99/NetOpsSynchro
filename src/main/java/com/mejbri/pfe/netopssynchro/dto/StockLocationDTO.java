package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.City;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLocationDTO {
    private Long id;
    private String name;
    private String description;
    private City city;
    private double latitude;
    private double longitude;
    private boolean active;

}
