package com.example.invoicetracker.dto;

import lombok.*;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignupResponse {
    private String message;
    private UserDto user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserDto {
        private String userId;
        private String username;
        private String email;
        private Set<String> roles;
    }
}
