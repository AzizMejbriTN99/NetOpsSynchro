package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "managed_databases")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ManagedDatabase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DatabaseType type;

    private String encryptedConnectionString;
    private String dbUsername;
    private String encryptedDbPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = true)
    private ManagedServer server;
}
