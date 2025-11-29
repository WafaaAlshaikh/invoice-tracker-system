package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.*;
import com.example.invoicetracker.exception.DuplicateUserException;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.model.entity.Role;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.repository.RoleRepository;
import com.example.invoicetracker.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    // Helper methods for creating test objects
    //DTO
    private UserRequest createUserRequest(String userId, String username, String email, String password,
            Set<String> roles) {
        UserRequest request = new UserRequest();
        request.setUserId(userId);
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setRoles(roles);
        return request;
    }

    //Repo
    private User createUser(String userId, String username, String email, String password, Set<Role> roles,
            boolean isActive) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(roles);
        user.setIsActive(isActive);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        return role;
    }

    private UserUpdateRequest createUserUpdateRequest(String username, String email, Boolean isActive,
            Set<String> roles) {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setIsActive(isActive);
        request.setRoles(roles);
        return request;
    }

    private PasswordChangeRequest createPasswordChangeRequest(String currentPassword, String newPassword) {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Nested
    class CreateUserTests {
        @Test
        void createUser_success() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", Set.of("USER"));
            Role role = createRole("USER");

            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(role));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

            User savedUser = createUser("U1", "testuser", "test@example.com", "encodedPassword", Set.of(role), true);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
            assertTrue(response.getRoles().contains("USER"));
            assertTrue(response.getIsActive());

            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertEquals("encodedPassword", capturedUser.getPassword());
            assertTrue(capturedUser.getIsActive());
        }

        @Test
        void createUser_withDefaultRoleWhenNoRolesProvided() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", null);
            Role defaultRole = createRole("USER");

            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(defaultRole));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

            User savedUser = createUser("U1", "testuser", "test@example.com", "encodedPassword", Set.of(defaultRole),
                    true);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertTrue(response.getRoles().contains("USER"));
            verify(roleRepository).findByRoleName("USER");
        }

        @Test
        void createUser_withMultipleRoles() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password",
                    Set.of("USER", "ADMIN"));
            Role userRole = createRole("USER");
            Role adminRole = createRole("ADMIN");

            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

            User savedUser = createUser("U1", "testuser", "test@example.com", "encodedPassword",
                    Set.of(userRole, adminRole), true);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = userService.createUser(request);

            // Then
            assertTrue(response.getRoles().contains("USER"));
            assertTrue(response.getRoles().contains("ADMIN"));
            verify(roleRepository).findByRoleName("USER");
            verify(roleRepository).findByRoleName("ADMIN");
        }

        @Test
        void createUser_failsWhenDuplicateUserId() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", Set.of("USER"));
            when(userRepository.existsByUserId("U1")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> userService.createUser(request));
            assertEquals("User ID already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createUser_failsWhenDuplicateUsername() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", Set.of("USER"));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> userService.createUser(request));
            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createUser_failsWhenDuplicateEmail() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", Set.of("USER"));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> userService.createUser(request));
            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createUser_failsWhenRoleNotFound() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password",
                    Set.of("INVALID_ROLE"));
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("INVALID_ROLE")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.createUser(request));
            assertEquals("Role not found: INVALID_ROLE", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void createUser_failsWhenDefaultRoleNotFound() {
            // Given
            UserRequest request = createUserRequest("U1", "testuser", "test@example.com", "password", null);
            when(userRepository.existsByUserId("U1")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(roleRepository.findByRoleName("USER")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.createUser(request));
            assertEquals("Default USER role not found", exception.getMessage());
        }
    }

    @Nested
    class GetUserByUsernameTests {
        @Test
        void getUserByUsername_success() {
            // Given
            Role role = createRole("USER");
            User user = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            UserResponse response = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
            assertTrue(response.getRoles().contains("USER"));
            verify(userRepository).findByUsername("testuser");
        }

        @Test
        void getUserByUsername_notFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.getUserByUsername("nonexistent"));
            assertEquals("User not found: nonexistent", exception.getMessage());
        }
    }

    @Nested
    class ListUsersTests {
        @Test
        void listUsers_withActiveFilter() {
            // Given
            Role role = createRole("USER");
            User user = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            Page<User> userPage = new PageImpl<>(List.of(user));

            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortBy("username");
            filter.setDirection("ASC");
            filter.setIsActive(true);

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));

            when(userRepository.findAllByIsActive(true, pageable)).thenReturn(userPage);

            // When
            Page<UserResponse> result = userService.listUsers(filter);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("testuser", result.getContent().get(0).getUsername());
            verify(userRepository).findAllByIsActive(true, pageable);
            verify(userRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void listUsers_withoutActiveFilter() {
            // Given
            Role role = createRole("USER");
            User user = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            Page<User> userPage = new PageImpl<>(List.of(user));

            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortBy("username");
            filter.setDirection("ASC");
            filter.setIsActive(null);

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));

            when(userRepository.findAll(pageable)).thenReturn(userPage);

            // When
            Page<UserResponse> result = userService.listUsers(filter);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(userRepository).findAll(pageable);
            verify(userRepository, never()).findAllByIsActive(anyBoolean(), any(Pageable.class));
        }

        @Test
        void listUsers_withInactiveFilter() {
            // Given
            Role role = createRole("USER");
            User user = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), false);
            Page<User> userPage = new PageImpl<>(List.of(user));

            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortBy("username");
            filter.setDirection("DESC");
            filter.setIsActive(false);

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "username"));

            when(userRepository.findAllByIsActive(false, pageable)).thenReturn(userPage);

            // When
            Page<UserResponse> result = userService.listUsers(filter);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertFalse(result.getContent().get(0).getIsActive());
            verify(userRepository).findAllByIsActive(false, pageable);
            verify(userRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void listUsers_emptyResult() {
            // Given
            Page<User> emptyPage = new PageImpl<>(List.of());
            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(0);
            filter.setSize(10);
            filter.setSortBy("username");
            filter.setDirection("ASC");
            filter.setIsActive(true);

            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "username"));

            when(userRepository.findAllByIsActive(true, pageable)).thenReturn(emptyPage);

            // When
            Page<UserResponse> result = userService.listUsers(filter);

            // Then
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            assertTrue(result.getContent().isEmpty());
            verify(userRepository).findAllByIsActive(true, pageable);
            verify(userRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        void listUsers_withDifferentPagination() {
            // Given
            Role role = createRole("USER");
            User user1 = createUser("U1", "user1", "user1@example.com", "password", Set.of(role), true);
            User user2 = createUser("U2", "user2", "user2@example.com", "password", Set.of(role), true);
            Page<User> userPage = new PageImpl<>(List.of(user1, user2));

            UserFilterRequest filter = new UserFilterRequest();
            filter.setPage(1);
            filter.setSize(5);
            filter.setSortBy("email");
            filter.setDirection("DESC");
            filter.setIsActive(true);

            Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "email"));

            when(userRepository.findAllByIsActive(true, pageable)).thenReturn(userPage);

            // When
            Page<UserResponse> result = userService.listUsers(filter);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            verify(userRepository).findAllByIsActive(true, pageable);
            verify(userRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    class UpdateUserTests {
        @Test
        void updateUser_success() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);

            UserUpdateRequest updateRequest = createUserUpdateRequest("newusername", "new@example.com", false, null);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByUsername("newusername")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUser("testuser", updateRequest);

            // Then
            assertEquals("newusername", response.getUsername());
            assertEquals("new@example.com", response.getEmail());
            assertFalse(response.getIsActive());

            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertEquals("newusername", updatedUser.getUsername());
            assertEquals("new@example.com", updatedUser.getEmail());
            assertFalse(updatedUser.getIsActive());
        }

        @Test
        void updateUser_partialUpdate() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);

            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setEmail("new@example.com");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUser("testuser", updateRequest);

            // Then
            assertEquals("testuser", response.getUsername());
            assertEquals("new@example.com", response.getEmail());
            assertTrue(response.getIsActive()); 
        }

        @Test
        void updateUser_updateRoles() {
            // Given
            Role userRole = createRole("USER");
            Role adminRole = createRole("ADMIN");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(userRole), true);

            UserUpdateRequest updateRequest = createUserUpdateRequest(null, null, null, Set.of("ADMIN"));

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUser("testuser", updateRequest);

            // Then
            assertTrue(response.getRoles().contains("ADMIN"));
            assertFalse(response.getRoles().contains("USER"));

            verify(userRepository).save(userCaptor.capture());
            User updatedUser = userCaptor.getValue();
            assertEquals(1, updatedUser.getRoles().size());
            assertTrue(updatedUser.getRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN")));
        }

        @Test
        void updateUser_failsWhenUserNotFound() {
            // Given
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUser("nonexistent", updateRequest));
            assertEquals("User not found: nonexistent", exception.getMessage());
        }

        @Test
        void updateUser_failsWhenDuplicateUsername() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            UserUpdateRequest updateRequest = createUserUpdateRequest("existinguser", null, null, null);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> userService.updateUser("testuser", updateRequest));
            assertEquals("Username already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_failsWhenDuplicateEmail() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            UserUpdateRequest updateRequest = createUserUpdateRequest(null, "existing@example.com", null, null);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            // When & Then
            DuplicateUserException exception = assertThrows(DuplicateUserException.class,
                    () -> userService.updateUser("testuser", updateRequest));
            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void updateUser_failsWhenRoleNotFound() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            UserUpdateRequest updateRequest = createUserUpdateRequest(null, null, null, Set.of("INVALID_ROLE"));

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(roleRepository.findByRoleName("INVALID_ROLE")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUser("testuser", updateRequest));
            assertEquals("Role not found: INVALID_ROLE", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateUserRolesTests {
        @Test
        void updateUserRoles_success() {
            // Given
            Role userRole = createRole("USER");
            Role adminRole = createRole("ADMIN");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(userRole), true);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUserRoles("testuser", Set.of("ADMIN"));

            // Then
            assertTrue(response.getRoles().contains("ADMIN"));
            assertFalse(response.getRoles().contains("USER"));
        }

        @Test
        void updateUserRoles_withMultipleRoles() {
            // Given
            Role userRole = createRole("USER");
            Role adminRole = createRole("ADMIN");
            Role managerRole = createRole("MANAGER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(userRole), true);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(roleRepository.findByRoleName("MANAGER")).thenReturn(Optional.of(managerRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUserRoles("testuser", Set.of("ADMIN", "MANAGER"));

            // Then
            assertEquals(2, response.getRoles().size());
            assertTrue(response.getRoles().contains("ADMIN"));
            assertTrue(response.getRoles().contains("MANAGER"));
        }

        @Test
        void updateUserRoles_failsWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUserRoles("nonexistent", Set.of("ADMIN")));
            assertEquals("User not found: nonexistent", exception.getMessage());
        }

        @Test
        void updateUserRoles_failsWhenRoleNotFound() {
            // Given
            Role role = createRole("USER");
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(roleRepository.findByRoleName("INVALID_ROLE")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUserRoles("testuser", Set.of("INVALID_ROLE")));
            assertEquals("Role not found: INVALID_ROLE", exception.getMessage());
        }
    }

    @Nested
    class UpdateUserStatusTests {
        @Test
        void updateUserStatus_activateUser() {
            // Given
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(), false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUserStatus("testuser", true);

            // Then
            assertTrue(response.getIsActive());
            verify(userRepository).save(userCaptor.capture());
            assertTrue(userCaptor.getValue().getIsActive());
        }

        @Test
        void updateUserStatus_deactivateUser() {
            // Given
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(), true);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = userService.updateUserStatus("testuser", false);

            // Then
            assertFalse(response.getIsActive());
            verify(userRepository).save(userCaptor.capture());
            assertFalse(userCaptor.getValue().getIsActive());
        }

        @Test
        void updateUserStatus_failsWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.updateUserStatus("nonexistent", true));
            assertEquals("User not found: nonexistent", exception.getMessage());
        }
         @Test
    void updateUser_withEmptyRolesSet() {
        // Given
        Role userRole = createRole("USER");
        Role adminRole = createRole("ADMIN");
        User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(adminRole), true);
        
        UserUpdateRequest updateRequest = createUserUpdateRequest(null, null, null, Set.of());
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserResponse response = userService.updateUser("testuser", updateRequest);
        
        // Then
        assertTrue(response.getRoles().contains("USER")); 
        assertEquals(1, response.getRoles().size());
        
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(1, savedUser.getRoles().size());
        assertTrue(savedUser.getRoles().stream().anyMatch(role -> "USER".equals(role.getRoleName())));
    }
    
    @Test
    void updateUser_noChanges() {
        // Given
        User existingUser = createUserWithDefaults();
        UserUpdateRequest updateRequest = new UserUpdateRequest(); 
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserResponse response = userService.updateUser("testuser", updateRequest);
        
        // Then
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.getIsActive());
        
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertTrue(savedUser.getIsActive());
    }

    @Test
    void updateUser_sameEmail_shouldNotValidateOrUpdate() {
        // Given
        Role role = createRole("USER");
        User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
        
        UserUpdateRequest updateRequest = createUserUpdateRequest("newuser", "test@example.com", null, null);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserResponse response = userService.updateUser("testuser", updateRequest);
        
        // Then
        assertEquals("newuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).existsByUsername("newuser"); 
        
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("newuser", updatedUser.getUsername());
        assertEquals("test@example.com", updatedUser.getEmail()); 
    }

    @Test
    void updateUser_sameUsername_shouldNotValidateOrUpdate() {
        // Given
        Role role = createRole("USER");
        User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(role), true);
        
        UserUpdateRequest updateRequest = createUserUpdateRequest("testuser", "new@example.com", null, null);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserResponse response = userService.updateUser("testuser", updateRequest);
        
        // Then
        assertEquals("testuser", response.getUsername()); 
        assertEquals("new@example.com", response.getEmail()); 
        
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository).existsByEmail("new@example.com"); 
        
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("testuser", updatedUser.getUsername()); 
        assertEquals("new@example.com", updatedUser.getEmail()); 
    }
}

private User createUserWithDefaults() {
    Role defaultRole = createRole("USER");
    return createUser("U1", "testuser", "test@example.com", "password", Set.of(defaultRole), true);
}
    

    @Nested
    class DeleteUserTests {
        @Test
        void deleteUser_success() {
            // Given
            User existingUser = createUser("U1", "testuser", "test@example.com", "password", Set.of(), true);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.deleteUser("testuser");

            // Then
            verify(userRepository).save(userCaptor.capture());
            assertFalse(userCaptor.getValue().getIsActive());
        }

        @Test
        void deleteUser_failsWhenUserNotFound() {
            // Given
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.deleteUser("nonexistent"));
            assertEquals("User not found: nonexistent", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class ChangePasswordTests {
        @Test
        void changePassword_success() {
            // Given
            User existingUser = createUser("U1", "testuser", "test@example.com", "oldEncoded", Set.of(), true);
            PasswordChangeRequest request = createPasswordChangeRequest("oldPassword", "newPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("oldPassword", "oldEncoded")).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("newEncoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userService.changePassword("testuser", request);

            // Then
            verify(userRepository).save(userCaptor.capture());
            assertEquals("newEncoded", userCaptor.getValue().getPassword());
        }

        @Test
        void changePassword_failsWhenUserNotFound() {
            // Given
            PasswordChangeRequest request = createPasswordChangeRequest("oldPassword", "newPassword");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> userService.changePassword("nonexistent", request));
            assertEquals("User not found: nonexistent", exception.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void changePassword_failsWhenCurrentPasswordIncorrect() {
            // Given
            User existingUser = createUser("U1", "testuser", "test@example.com", "oldEncoded", Set.of(), true);
            PasswordChangeRequest request = createPasswordChangeRequest("wrongPassword", "newPassword");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("wrongPassword", "oldEncoded")).thenReturn(false);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.changePassword("testuser", request));
            assertEquals("Current password is incorrect", exception.getMessage());
            verify(userRepository, never()).save(any());
        }
    }
}