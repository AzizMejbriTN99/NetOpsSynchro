package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.dto.RegisterRequest;
import com.mejbri.pfe.netopssynchro.dto.UserDTO;
import com.mejbri.pfe.netopssynchro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id,
                                          @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted");
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<UserDTO> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toggleUser(id));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        userService.register(req);
        return ResponseEntity.ok("User created successfully");
    }
}
