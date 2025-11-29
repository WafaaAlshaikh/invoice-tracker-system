package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.LoginRequest;
import com.example.invoicetracker.dto.LoginResponse;
import com.example.invoicetracker.dto.SignupRequest;
import com.example.invoicetracker.dto.SignupResponse;
import com.example.invoicetracker.exception.*;
import com.example.invoicetracker.model.entity.Role;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.repository.RoleRepository;
import com.example.invoicetracker.repository.UserRepository;
import com.example.invoicetracker.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Role userRole;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setup() {
        userRole = Role.builder()
                .roleName("USER")
                .build();

        activeUser = User.builder()
                .userId("U1")
                .username("user1")
                .email("user1@example.com")
                .password("encodedPassword")
                .roles(Set.of(userRole))
                .isActive(true)
                .build();

        inactiveUser = User.builder()
                .userId("U2")
                .username("user2")
                .email("user2@example.com")
                .password("encodedPassword")
                .roles(Set.of(userRole))
                .isActive(false)
                .build();
    }

    @Nested
    class RegisterUserTests {
        @Test
        void registerUser_success() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");

            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setIsActive(true); 
                return user;
            });

            // When
            SignupResponse response = authService.registerUser(request);

            // Then
            assertNotNull(response);
            assertEquals("User registered successfully", response.getMessage());
            assertEquals("user1", response.getUser().getUsername());
            assertEquals("user1@example.com", response.getUser().getEmail());
            assertTrue(response.getUser().getRoles().contains("USER"));

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertEquals("encodedPassword", savedUser.getPassword());
            assertTrue(savedUser.getIsActive());
        }

        @Test
        void registerUser_fails_whenDuplicateUserId() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> authService.registerUser(request));
            assertEquals("User ID, username, or email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void registerUser_fails_whenDuplicateUsername() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> authService.registerUser(request));
            assertEquals("User ID, username, or email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void registerUser_fails_whenDuplicateEmail() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByEmail("user1@example.com")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> authService.registerUser(request));
            assertEquals("User ID, username, or email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void registerUser_fails_whenDefaultRoleNotFound() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.registerUser(request));
            assertEquals("Default USER role not found", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void registerUser_fails_whenDatabaseException() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> authService.registerUser(request));
            assertEquals("User ID, username, or email already exists or invalid data", exception.getMessage());
        }

        @Test
        void registerUser_setsUserActiveByDefault() {
            // Given
            SignupRequest request = createSignupRequest("U1", "user1", "user1@example.com", "password");
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("user1")).thenReturn(false);
            when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setIsActive(true);
                return user;
            });

            // When
            authService.registerUser(request);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertTrue(savedUser.getIsActive());
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_success_withMultipleRoles() {
            // Given
            Role adminRole = Role.builder().roleName("ADMIN").build();
            User userWithMultipleRoles = User.builder()
                    .username("admin")
                    .password("encodedPassword")
                    .roles(Set.of(userRole, adminRole))
                    .isActive(true)
                    .build();

            LoginRequest request = createLoginRequest("admin", "password");
            when(loginAttemptService.isBlocked("admin")).thenReturn(false);
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(userWithMultipleRoles));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("admin", Set.of("USER", "ADMIN"))).thenReturn("jwt-token");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertTrue(response.getUser().getRoles().contains("USER"));
            assertTrue(response.getUser().getRoles().contains("ADMIN"));
        }

        @Test
        void login_fails_whenUserBlocked() {
            // Given
            LoginRequest request = createLoginRequest("user1", "password");
            when(loginAttemptService.isBlocked("user1")).thenReturn(true);

            // When & Then
            TooManyAttemptsException exception = assertThrows(TooManyAttemptsException.class,
                    () -> authService.login(request));
            assertEquals("Too many failed login attempts. Try again later.", exception.getMessage());

            verify(loginAttemptService, never()).loginSucceeded(anyString());
            verify(loginAttemptService, never()).loginFailed(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        void login_fails_whenWhitespaceUsername() {
            // Given
            LoginRequest request = createLoginRequest("   ", "password");

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService).loginFailed("   ");
            verify(loginAttemptService, never()).isBlocked(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        void login_fails_whenUserNotFound() {
            // Given
            LoginRequest request = createLoginRequest("nonexistent", "password");
            when(loginAttemptService.isBlocked("nonexistent")).thenReturn(false);
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService, never()).loginFailed(anyString());
            verify(loginAttemptService, never()).loginSucceeded(anyString());
        }

        @Test
        void login_success() {
            // Given
            LoginRequest request = createLoginRequest("user1", "password");
            when(loginAttemptService.isBlocked("user1")).thenReturn(false);
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("user1", Set.of("USER"))).thenReturn("jwt-token");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals("user1", response.getUser().getUsername());
            assertEquals("user1@example.com", response.getUser().getEmail());
            assertTrue(response.getUser().getRoles().contains("USER"));

            verify(loginAttemptService).loginSucceeded("user1");
            verify(loginAttemptService, never()).loginFailed(anyString());
        }

        @Test
        void login_fails_whenWrongPassword() {
            // Given
            LoginRequest request = createLoginRequest("user1", "wrongpassword");
            when(loginAttemptService.isBlocked("user1")).thenReturn(false);
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService).loginFailed("user1");
            verify(loginAttemptService, never()).loginSucceeded(anyString());
        }

        @Test
        void login_fails_whenUserDeactivated() {
            // Given
            LoginRequest request = createLoginRequest("user2", "password");
            when(loginAttemptService.isBlocked("user2")).thenReturn(false);
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(inactiveUser));

            // When & Then
            UserDeactivatedException exception = assertThrows(UserDeactivatedException.class,
                    () -> authService.login(request));
            assertEquals("User account is deactivated", exception.getMessage());

            verify(loginAttemptService, never()).loginFailed(anyString());
            verify(loginAttemptService, never()).loginSucceeded(anyString());
        }

        @Test
        void login_fails_whenEmptyUsername() {
            // Given
            LoginRequest request = createLoginRequest("", "password");

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService).loginFailed("");
            verify(loginAttemptService, never()).isBlocked(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        void login_fails_whenNullUsername() {
            // Given
            LoginRequest request = createLoginRequest(null, "password");

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService).loginFailed("null");
            verify(loginAttemptService, never()).isBlocked(anyString());
            verify(userRepository, never()).findByUsername(anyString());
        }

        @Test
        void login_fails_whenNullPassword() {
            // Given
            LoginRequest request = createLoginRequest("user1", null);
            when(loginAttemptService.isBlocked("user1")).thenReturn(false);
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(null, "encodedPassword")).thenReturn(false);

            // When & Then
            InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
            assertEquals("Invalid username or password", exception.getMessage());

            verify(loginAttemptService).loginFailed("user1");
        }
    }

    @Nested
    class SecurityTests {
        @Test
        void login_incrementsFailedAttemptsOnWrongPassword() {
            // Given
            LoginRequest request = createLoginRequest("user1", "wrongpassword");
            when(loginAttemptService.isBlocked("user1")).thenReturn(false);
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            // When & Then
            assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
            verify(loginAttemptService).loginFailed("user1");
        }

        @Test
        void login_resetsAttemptsOnSuccess() {
            // Given
            LoginRequest request = createLoginRequest("user1", "password");
            when(loginAttemptService.isBlocked("user1")).thenReturn(false);
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken("user1", Set.of("USER"))).thenReturn("jwt-token");

            // When
            authService.login(request);

            // Then
            verify(loginAttemptService).loginSucceeded("user1");
        }

        @Test
        void login_doesNotIncrementAttemptsOnDeactivatedUser() {
            // Given
            LoginRequest request = createLoginRequest("user2", "password");
            when(loginAttemptService.isBlocked("user2")).thenReturn(false);
            when(userRepository.findByUsername("user2")).thenReturn(Optional.of(inactiveUser));

            // When & Then
            assertThrows(UserDeactivatedException.class, () -> authService.login(request));
            verify(loginAttemptService, never()).loginFailed("user2");
            verify(loginAttemptService, never()).loginSucceeded("user2");
        }
    }

    @Nested
    class EdgeCasesTests {
        @Test
        void registerUser_withVeryLongCredentials() {
            // Given
            String longUserId = "U".repeat(100);
            String longUsername = "user".repeat(50);
            String longEmail = "email".repeat(20) + "@example.com";

            SignupRequest request = createSignupRequest(longUserId, longUsername, longEmail, "password");

            when(userRepository.existsByUserId(longUserId)).thenReturn(false);
            when(userRepository.existsByUsername(longUsername)).thenReturn(false);
            when(userRepository.existsByEmail(longEmail)).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            SignupResponse response = authService.registerUser(request);

            // Then
            assertNotNull(response);
            assertEquals(longUsername, response.getUser().getUsername());
            assertEquals(longEmail, response.getUser().getEmail());
        }

        @Test
        void login_withSpecialCharactersInUsername() {
            // Given
            String specialUsername = "user-123_test";
            User specialUser = User.builder()
                    .username(specialUsername)
                    .password("encodedPassword")
                    .roles(Set.of(userRole))
                    .isActive(true)
                    .build();

            LoginRequest request = createLoginRequest(specialUsername, "password");
            when(loginAttemptService.isBlocked(specialUsername)).thenReturn(false);
            when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(specialUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtUtil.generateToken(specialUsername, Set.of("USER"))).thenReturn("jwt-token");

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertNotNull(response);
            assertEquals(specialUsername, response.getUser().getUsername());
        }
    }

    // Helper methods
    private SignupRequest createSignupRequest(String userId, String username, String email, String password) {
        SignupRequest request = new SignupRequest();
        request.setUserId(userId);
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private LoginRequest createLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}