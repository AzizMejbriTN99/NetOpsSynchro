package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.ProfileResponse;
import com.mejbri.pfe.netopssynchro.dto.ProfileUpdateRequest;
import com.mejbri.pfe.netopssynchro.entity.User;
import com.mejbri.pfe.netopssynchro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getProfile(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(), request));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        String ct = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        if (!ct.startsWith("image/"))
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(
                userService.uploadAvatar(authentication.getName(), file.getBytes(), ct));
    }

    @GetMapping("/profile/avatar")
    public ResponseEntity<byte[]> getAvatar(Authentication authentication) {
        User user = userService.getRawUser(authentication.getName());
        if (user.getAvatarData() == null || user.getAvatarData().length == 0)
            return ResponseEntity.notFound().build();

        String ct = user.getAvatarContentType() != null
                ? user.getAvatarContentType() : "image/jpeg";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(user.getAvatarData());
    }

    @DeleteMapping("/profile/avatar")
    public ResponseEntity<ProfileResponse> deleteAvatar(Authentication authentication) {
        return ResponseEntity.ok(userService.deleteAvatar(authentication.getName()));
    }

}
