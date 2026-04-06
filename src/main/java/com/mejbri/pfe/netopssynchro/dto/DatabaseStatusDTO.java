package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DatabaseType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatabaseStatusDTO {
    private Long databaseId;
    private String name;
    private DatabaseType type;
    private Long serverId;
    private boolean connected;
}
