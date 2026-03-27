package com.mejbri.pfe.netopssynchro.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileRequest {
    private String username;
    private String profileName;
}
