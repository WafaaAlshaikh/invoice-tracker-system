package com.example.invoicetracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("===== JWT FILTER START =====");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Authorization header: {}", request.getHeader("Authorization"));

        String token = extractTokenFromRequest(request);

        if (token != null) {
            log.info("Token found: {}", token);

            boolean valid = jwtUtil.validateToken(token);
            log.info("Token valid: {}", valid);

            if (valid) {
                String username = jwtUtil.getUsernameFromToken(token);
                Set<String> roles = jwtUtil.getRolesFromToken(token);

                log.info("Authenticated username: {}", username);
                log.info("Roles from token: {}", roles);

                Set<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username,
                        null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("SecurityContext updated with authentication");
            } else {
                log.warn("Invalid token, skipping authentication");
            }
        } else {
            log.info("No token found in request");
        }

        filterChain.doFilter(request, response);
        log.info("===== JWT FILTER END =====");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
