package com.example.invoicetracker.controller;

import com.example.invoicetracker.dto.*;
import com.example.invoicetracker.service.UserService;
import lombok.RequiredArgsConstructor;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('SUPERUSER')")
    @PostMapping
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user account (SUPERUSER only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "User details", required = true)
            @Validated @RequestBody UserRequest request) {
        return ResponseEntity.status(201).body(userService.createUser(request));
    }

    @PreAuthorize("hasRole('SUPERUSER')")
    @GetMapping
    @Operation(
        summary = "List users",
        description = "Retrieves a paginated list of users (SUPERUSER only)"
    )
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    public ResponseEntity<Page<UserResponse>> listUsers(
            @Parameter(description = "Filter criteria")
            UserFilterRequest filter) {
        return ResponseEntity.ok(userService.listUsers(filter));
    }

    @PreAuthorize("hasRole('SUPERUSER') or hasRole('AUDITOR') or #username == authentication.name")
    @GetMapping("/{username}")
    @Operation(
        summary = "Get user by username",
        description = "Retrieves a specific user by username"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PreAuthorize("hasRole('SUPERUSER') or #username == authentication.name")
    @PutMapping("/{username}")
    @Operation(
        summary = "Update user",
        description = "Updates user information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Username/email already exists"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Parameter(description = "Updated user details", required = true)
            @Validated @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(username, request));
    }

    @PreAuthorize("hasRole('SUPERUSER')")
    @DeleteMapping("/{username}")
    @Operation(
        summary = "Delete user",
        description = "Soft deletes a user (SUPERUSER only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('SUPERUSER')")
    @PutMapping("/{username}/roles")
    @Operation(
        summary = "Update user roles",
        description = "Updates roles for a user (SUPERUSER only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Parameter(description = "Set of role names", required = true)
            @RequestBody Set<String> roles) {
        return ResponseEntity.ok(userService.updateUserRoles(username, roles));
    }

    @PreAuthorize("hasRole('SUPERUSER')")
    @PutMapping("/{username}/status")
    @Operation(
        summary = "Update user status",
        description = "Activates or deactivates a user (SUPERUSER only)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Parameter(description = "Active status", required = true)
            @RequestParam Boolean active) {
        return ResponseEntity.ok(userService.updateUserStatus(username, active));
    }

    @PreAuthorize("hasRole('SUPERUSER') or #username == authentication.name")
    @PutMapping("/{username}/password")
    @Operation(
        summary = "Change password",
        description = "Changes user password"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid current password"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Parameter(description = "Password change details", required = true)
            @RequestBody PasswordChangeRequest request) {
        userService.changePassword(username, request);
        return ResponseEntity.noContent().build();
    }
}