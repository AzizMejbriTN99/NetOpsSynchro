package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_photos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DemandePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private Demande demande;

    private String filename;
    private String storagePath;
    private String uploadedBy;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() { this.uploadedAt = LocalDateTime.now(); }
}
