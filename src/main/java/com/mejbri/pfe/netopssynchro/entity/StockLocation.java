package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stock_locations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StockLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private City city;

    private double latitude;
    private double longitude;

    private boolean active = true;
}
