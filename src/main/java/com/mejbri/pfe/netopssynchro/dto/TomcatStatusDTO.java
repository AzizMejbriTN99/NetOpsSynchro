package com.mejbri.pfe.netopssynchro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TomcatStatusDTO {
    private Long instanceId;
    private String name;
    private Long serverId;
    private boolean running;
    private List<String> webapps;
    private List<String> logFiles;
}
