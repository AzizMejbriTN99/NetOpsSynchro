package com.mejbri.pfe.netopssynchro.dto;

import com.mejbri.pfe.netopssynchro.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
}
