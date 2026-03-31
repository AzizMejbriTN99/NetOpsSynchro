package com.mejbri.pfe.netopssynchro.controller;

import com.mejbri.pfe.netopssynchro.config.JwtService;
import com.mejbri.pfe.netopssynchro.dto.LoginRequest;
import com.mejbri.pfe.netopssynchro.dto.LoginResponse;
import com.mejbri.pfe.netopssynchro.dto.RegisterRequest;
import com.mejbri.pfe.netopssynchro.entity.LoginEvent;
import com.mejbri.pfe.netopssynchro.entity.User;
import com.mejbri.pfe.netopssynchro.repository.LoginEventRepository;
import com.mejbri.pfe.netopssynchro.repository.UserRepository.UserRepository;
import com.mejbri.pfe.netopssynchro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final LoginEventRepository loginEventRepository;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        var userDetails = userDetailsService.loadUserByUsername(req.getUsername());
        String token = jwtService.generateToken(userDetails);
        String roleStr = userDetails.getAuthorities().iterator().next().getAuthority();

        // save login event
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        loginEventRepository.save(LoginEvent.builder()
                .username(req.getUsername())
                .role(user.getRole())
                .build());

        return ResponseEntity.ok(new LoginResponse(token, roleStr, userDetails.getUsername()));
    }

}
