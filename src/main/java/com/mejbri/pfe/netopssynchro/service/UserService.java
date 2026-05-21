package com.mejbri.pfe.netopssynchro.service;

import com.mejbri.pfe.netopssynchro.dto.ProfileResponse;
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
                .firstname(req.getFirstname())
                .lastname(req.getLastname())
                .phone(req.getPhone())
                .city(req.getCity())
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
        user.setFirstname(req.getFirstname());
        user.setLastname(req.getLastname());
        user.setPhone(req.getPhone());
        user.setCity(req.getCity());
        user.setRole(req.getRole());
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        UserDTO dto = toDTO(userRepository.save(user));
        notificationService.push(NotificationType.USER_UPDATED, user.getUsername());
        return dto;
    }


    public ProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toProfileResponse(user, null);
    }

    public ProfileResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstname() != null && !request.getFirstname().isBlank())
            user.setFirstname(request.getFirstname());

        if (request.getLastname() != null && !request.getLastname().isBlank())
            user.setLastname(request.getLastname());

        if (request.getEmail() != null && !request.getEmail().isBlank())
            user.setEmail(request.getEmail());

        if (request.getPhone() != null && !request.getPhone().isBlank())
            user.setPhone(request.getPhone());

        boolean wantsPasswordChange = request.getNewPassword() != null
                && !request.getNewPassword().isBlank();

        if (wantsPasswordChange) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
                throw new RuntimeException("Current password is incorrect");
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        return toProfileResponse(userRepository.save(user), "Profile updated successfully");
    }

    public ProfileResponse uploadAvatar(String username, byte[] data, String contentType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarData(data);
        user.setAvatarContentType(contentType);
        return toProfileResponse(userRepository.save(user), "Avatar updated successfully");
    }

    public ProfileResponse deleteAvatar(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAvatarData(null);
        user.setAvatarContentType(null);
        return toProfileResponse(userRepository.save(user), "Avatar removed");
    }

    public User getRawUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ProfileResponse toProfileResponse(User u, String message) {
        return ProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .firstname(u.getFirstname())
                .lastname(u.getLastname())
                .phone(u.getPhone())
                .city(u.getCity())
                .role(u.getRole())
                .hasAvatar(u.getAvatarData() != null && u.getAvatarData().length > 0)
                .createdAt(u.getCreatedAt())
                .message(message)
                .build();
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
        dto.setFirstname(u.getFirstname());
        dto.setLastname(u.getLastname());
        dto.setPhone(u.getPhone());
        dto.setCity(u.getCity());
        dto.setRole(u.getRole());
        dto.setHasAvatar(u.getAvatarData() != null && u.getAvatarData().length > 0);
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        dto.setEnabled(u.isEnabled());
        return dto;
    }
}