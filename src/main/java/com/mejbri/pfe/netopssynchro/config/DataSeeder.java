package com.mejbri.pfe.netopssynchro.config;

import com.mejbri.pfe.netopssynchro.entity.Role;
import com.mejbri.pfe.netopssynchro.entity.User;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("admin")) return;

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin1234"))
                .email("admin@netops.int")
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);
        System.out.println(">>> Default admin user created: admin / admin1234");
    }
}
