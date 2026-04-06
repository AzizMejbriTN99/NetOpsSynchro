package com.mejbri.pfe.netopssynchro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TomcatInstanceDTO {
    private Long id;
    private String name;
    private String catalinaHome;
    private String webappsPath;
    private String logsPath;
    private String startScript;
    private String stopScript;
    private Long serverId;
}
