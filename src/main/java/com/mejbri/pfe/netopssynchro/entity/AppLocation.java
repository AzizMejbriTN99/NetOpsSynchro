package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_locations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private City city;

    @Enumerated(EnumType.STRING)
    private LocationType type;

    private double latitude;
    private double longitude;

    private Double minLat;
    private Double maxLat;
    private Double minLng;
    private Double maxLng;

    @Builder.Default
    private boolean active = true;
}
