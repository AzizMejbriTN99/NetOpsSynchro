package com.mejbri.pfe.netopssynchro.repository.UserRepository;

import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(Role role);
}
