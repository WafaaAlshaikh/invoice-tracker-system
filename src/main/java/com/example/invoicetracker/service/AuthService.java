package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.LoginRequest;
import com.example.invoicetracker.dto.LoginResponse;
import com.example.invoicetracker.dto.SignupRequest;
import com.example.invoicetracker.dto.SignupResponse;
import com.example.invoicetracker.exception.DuplicateUserException;
import com.example.invoicetracker.exception.InvalidCredentialsException;
import com.example.invoicetracker.exception.TooManyAttemptsException;
import com.example.invoicetracker.exception.UserDeactivatedException;
import com.example.invoicetracker.model.entity.Role;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.repository.RoleRepository;
import com.example.invoicetracker.repository.UserRepository;
import com.example.invoicetracker.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginAttemptService loginAttemptService;


    @Transactional
    public SignupResponse registerUser(SignupRequest request) {

        if (userRepository.existsByUserId(request.getUserId())) {
        throw new DuplicateUserException("User ID already exists");
    }

    if (userRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateUserException("Username already exists");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateUserException("Email already exists");
    }
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("Default USER role not found"));

        Set<Role> roles = Set.of(userRole);

        User user = User.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();

        try {
            userRepository.save(user); 
        } catch (Exception e) {
            throw new DuplicateUserException("User ID, username, or email already exists or invalid data");
        }

        SignupResponse.UserDto userDto = SignupResponse.UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles.stream().map(Role::getRoleName).collect(Collectors.toSet()))
                .build();

        return SignupResponse.builder()
                .message("User registered successfully")
                .user(userDto)
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (loginAttemptService.isBlocked(request.getUsername())) {
            throw new TooManyAttemptsException("Too many failed login attempts. Try again later.");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

         if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UserDeactivatedException("User account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        loginAttemptService.loginSucceeded(request.getUsername());

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());

        String token = jwtUtil.generateToken(user.getUsername(), roleNames);

        LoginResponse.UserDto userDto = LoginResponse.UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roleNames)
                .build();

        return LoginResponse.builder()
                .token(token)
                .user(userDto)
                .build();
    }
}


