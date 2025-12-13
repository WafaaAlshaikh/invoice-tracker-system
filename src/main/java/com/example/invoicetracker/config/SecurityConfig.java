package com.example.invoicetracker.config;

import com.example.invoicetracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity  
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("SecurityConfig ");
        
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        }));

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ==================== PUBLIC ENDPOINTS ====================
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        
                        // ==================== ACTUATOR ENDPOINTS ====================
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // ==================== CUSTOM HEALTH ENDPOINTS ====================
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/status").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/status").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        
                        // ==================== SWAGGER ====================
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        
                        // ==================== AI ENDPOINTS ====================
                        .requestMatchers("/ai/test/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/invoices/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/chat/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/charts/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        
                        // ==================== USER MANAGEMENT ====================
                        .requestMatchers("/users/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        
                        // ==================== CATEGORIES & PRODUCTS ====================
                        .requestMatchers("/categories/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        .requestMatchers("/products/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        
                        // ==================== INVOICES ====================
                        .requestMatchers("/invoices/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        
                        // ==================== DEBUG ====================
                        .requestMatchers("/ai/debug/**").permitAll()
                        
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info(" SecurityConfig finish ");
        return http.build();
    }
}