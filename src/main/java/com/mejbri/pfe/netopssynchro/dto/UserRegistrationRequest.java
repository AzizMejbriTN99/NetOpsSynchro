package com.mejbri.pfe.netopssynchro.dto;

public class UserRegistrationRequest {
    private String username;
    private String password;
    private String email;
    private String role;
    private String displayName;
    private boolean isLdapUser;

    public UserRegistrationRequest(String username, String password, String email, String role, String displayName, boolean isLdapUser) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.displayName = displayName;
        this.isLdapUser = isLdapUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isLdapUser() {
        return isLdapUser;
    }

    public void setLdapUser(boolean ldapUser) {
        isLdapUser = ldapUser;
    }
}
