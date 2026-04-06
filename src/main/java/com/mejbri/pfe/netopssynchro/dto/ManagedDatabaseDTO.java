package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedDatabaseDTO {
    private Long id;
    private String name;
    private DatabaseType type;
    private String connectionString;
    private String dbUsername;
    private String dbPassword;
    private Long serverId;
}
