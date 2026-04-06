package com.mejbri.pfe.netopssynchro.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerStatusDTO {
    private Long serverId;
    private String name;
    private String host;
    private boolean sshReachable;
}
