package com.mejbri.pfe.netopssynchro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private String firstname;
    private String lastname;
    private String phone;

    @Column(length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean enabled = true;

    @PrePersist  public void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   public void preUpdate()  { updatedAt = LocalDateTime.now(); }
}
