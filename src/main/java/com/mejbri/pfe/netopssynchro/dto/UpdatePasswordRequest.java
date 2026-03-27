package com.mejbri.pfe.netopssynchro.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class UpdatePasswordRequest {
    private String username;
    private String password;
    private String newPassword;
}
