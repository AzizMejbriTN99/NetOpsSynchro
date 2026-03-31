package com.mejbri.pfe.netopssynchro.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "technician_location_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TechnicianLocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    private double latitude;
    private double longitude;

    @Enumerated(EnumType.STRING)
    private City city;

    private LocalDateTime recordedAt;

    @PrePersist
    public void prePersist() {
        this.recordedAt = LocalDateTime.now();
    }
}
