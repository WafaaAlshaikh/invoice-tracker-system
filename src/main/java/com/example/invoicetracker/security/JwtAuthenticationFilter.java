package com.example.invoicetracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        System.out.println("=== JWT FILTER EXECUTING ===");
        System.out.println("Request URI: " + request.getRequestURI());
        
        String token = extractTokenFromRequest(request);
        System.out.println("Token extracted: " + (token != null ? "YES" : "NO"));
        
        if (token != null) {
            System.out.println("Token: " + token.substring(0, Math.min(20, token.length())) + "...");
            boolean valid = jwtUtil.validateToken(token);
            System.out.println("Token valid: " + valid);
            
            if (valid) {
                String username = jwtUtil.getUsernameFromToken(token);
                Set<String> roles = jwtUtil.getRolesFromToken(token);
                System.out.println("Username: " + username);
                System.out.println("Roles: " + roles);
                
                Set<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());
                
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication SET in SecurityContext");
            }
        } else {
            System.out.println("No token found in request");
        }
        
        System.out.println("=== JWT FILTER FINISHED ===");
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}