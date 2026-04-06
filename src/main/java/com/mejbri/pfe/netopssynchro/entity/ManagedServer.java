package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "managed_servers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ManagedServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String host;
    private int port;
    private String username;
    private String encryptedPassword;

    @Enumerated(EnumType.STRING)
    private City city;

    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TomcatInstance> tomcatInstances;

    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManagedDatabase> databases;
}
