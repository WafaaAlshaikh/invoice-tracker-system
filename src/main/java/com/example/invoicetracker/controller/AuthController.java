package com.example.invoicetracker.controller;

import com.example.invoicetracker.dto.LoginRequest;
import com.example.invoicetracker.dto.LoginResponse;
import com.example.invoicetracker.dto.SignupRequest;
import com.example.invoicetracker.dto.SignupResponse;
import com.example.invoicetracker.security.JwtUtil;
import com.example.invoicetracker.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.registerUser(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            Set<String> roles = jwtUtil.getRolesFromToken(token);

            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "roles", roles,
                    "valid", true));
        }

        return ResponseEntity.status(401).body(Map.of("valid", false));
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
