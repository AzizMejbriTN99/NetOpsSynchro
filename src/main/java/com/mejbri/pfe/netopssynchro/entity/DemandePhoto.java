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
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /**
     * The file bytes stored as a LONGBLOB.
     * LAZY fetch so the blob is NOT loaded when listing photo metadata —
     * only fetched when actually serving the file.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", columnDefinition = "LONGBLOB", nullable = false)
    private byte[] data;

    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) this.uploadedAt = LocalDateTime.now();
    }
}