package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "technician_resources")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TechnicianResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    private String resourceName;
    private int quantity;
    private String unit;
    private String notes;
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() { this.addedAt = LocalDateTime.now(); }
}
