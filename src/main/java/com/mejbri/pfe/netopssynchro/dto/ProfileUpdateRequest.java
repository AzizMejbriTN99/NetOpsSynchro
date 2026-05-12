package com.mejbri.pfe.netopssynchro.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String email;
    private String phone;
    private String currentPassword;
    private String newPassword;
}
