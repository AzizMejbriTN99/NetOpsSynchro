package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.City;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagedServerDTO {
    private Long id;
    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private City city;
    private boolean active;
}
