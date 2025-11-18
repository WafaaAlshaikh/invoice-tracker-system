package com.example.invoicetracker.config;

import com.example.invoicetracker.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
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

        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(List.of("http://localhost:3000"));
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setAllowCredentials(true);
            return config;
        }));

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers("/ai/test/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/invoices/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/chat/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/charts/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/users/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        .requestMatchers("/categories/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        .requestMatchers("/products/**").hasAnyRole("SUPERUSER", "AUDITOR")
                        .requestMatchers("/invoices/**").hasAnyRole("USER", "SUPERUSER", "AUDITOR")
                        .requestMatchers("/ai/debug/**").permitAll() 
                        .anyRequest().authenticated())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
