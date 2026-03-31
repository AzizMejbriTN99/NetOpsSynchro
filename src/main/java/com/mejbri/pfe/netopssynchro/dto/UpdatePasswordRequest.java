package com.mejbri.pfe.netopssynchro.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdatePasswordRequest {
    private String username;
    private String password;
    private String newPassword;
}
