package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tomcat_instances")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TomcatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String catalinaHome;
    private String webappsPath;
    private String logsPath;
    private String startScript;
    private String stopScript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id")
    private ManagedServer server;
}
