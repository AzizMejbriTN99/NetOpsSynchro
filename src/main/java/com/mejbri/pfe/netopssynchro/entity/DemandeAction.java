package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_actions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DemandeAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private Demande demande;

    @Enumerated(EnumType.STRING)
    private DemandeActionStatus status;

    private String note;

    private LocalDateTime performedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @PrePersist
    public void prePersist() {
        this.performedAt = LocalDateTime.now();
    }
}