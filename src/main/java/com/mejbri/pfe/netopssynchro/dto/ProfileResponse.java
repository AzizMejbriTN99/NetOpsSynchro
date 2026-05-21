package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.City;
import com.mejbri.pfe.netopssynchro.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfileResponse {
    private Long      id;
    private String    username;
    private String    email;
    private String    firstname;
    private String    lastname;
    private String    phone;
    private City      city;
    private Role      role;
    private boolean   hasAvatar;
    private LocalDateTime createdAt;
    private String    message;
}