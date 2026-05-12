package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.ProfileUpdateRequest;
import com.mejbri.pfe.netopssynchro.dto.RegisterRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.entity.NotificationType;
import com.mejbri.pfe.netopssynchro.entity.User;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new RuntimeException("Username already taken");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already in use");

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .role(req.getRole())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        notificationService.push(NotificationType.USER_CREATED, saved.getUsername());
        return saved;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    public UserDTO updateUser(Long id, RegisterRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmail(req.getEmail());
        user.setRole(req.getRole());
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        UserDTO dto = toDTO(userRepository.save(user));
        notificationService.push(NotificationType.USER_UPDATED, user.getUsername());
        return dto;
    }


    public Map<String, String> updateProfile(
            String username,
            ProfileUpdateRequest request
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null &&
                !request.getEmail().isBlank()) {

            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null &&
                !request.getPhone().isBlank()) {

            user.setPhone(request.getPhone());
        }

        boolean wantsPasswordChange =
                request.getNewPassword() != null &&
                        !request.getNewPassword().isBlank();

        if (wantsPasswordChange) {

            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(
                            request.getCurrentPassword(),
                            user.getPassword()
                    )) {

                throw new RuntimeException("Current password is incorrect");
            }

            user.setPassword(
                    passwordEncoder.encode(request.getNewPassword())
            );
        }

        userRepository.save(user);

        return Map.of(
                "message",
                "Profile updated successfully"
        );
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        notificationService.push(NotificationType.USER_DELETED, user.getUsername());
        userRepository.deleteById(id);
    }

    public UserDTO toggleUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        UserDTO dto = toDTO(userRepository.save(user));
        notificationService.push(NotificationType.USER_TOGGLED, user.getUsername());
        return dto;
    }

    private UserDTO toDTO(User u) {
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        dto.setEnabled(u.isEnabled());
        return dto;
    }
}