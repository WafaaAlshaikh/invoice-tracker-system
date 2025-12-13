package com.example.invoicetracker.controller.util;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthHelper {

    public String extractRole(Authentication authentication) {
        return authentication.getAuthorities()
                             .iterator()
                             .next()
                             .getAuthority()
                             .replace("ROLE_", "");
    }

    public String extractUsername(Authentication authentication) {
        return authentication.getName();
    }

    public Map<String, String> getUserInfo(Authentication authentication) {
        Map<String, String> info = new HashMap<>();
        info.put("username", extractUsername(authentication));
        info.put("role", extractRole(authentication));
        return info;
    }
}
