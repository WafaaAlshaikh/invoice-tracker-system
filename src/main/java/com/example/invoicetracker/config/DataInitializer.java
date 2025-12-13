package com.example.invoicetracker.config;

import com.example.invoicetracker.model.entity.Role;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.repository.RoleRepository;
import com.example.invoicetracker.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        createRoles();
        createSuperUser();
    }

    private void createRoles() {
        List<String> roleNames = List.of("USER", "SUPERUSER", "AUDITOR");
        for (String roleName : roleNames) {
            try {
                if (roleRepository.findByRoleName(roleName).isEmpty()) {
                    Role role = Role.builder().roleName(roleName).build();
                    roleRepository.save(role);
                    log.info("Created role: {}", roleName);
                }
            } catch (DataIntegrityViolationException ex) {
                log.warn("Role {} might already exist: {}", roleName, ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed to create role {}: {}", roleName, ex.getMessage());
            }
        }
    }

    private void createSuperUser() {
        if (userRepository.findByUsername("superuser").isEmpty()) {
            Set<Role> superUserRoles = roleRepository.findAll().stream()
                    .collect(Collectors.toSet());

            User superUser = User.builder()
                    .userId("SUPER001")
                    .username("superuser")
                    .email("superuser@system.com")
                    .password(passwordEncoder.encode("SuperUser123!"))
                    .roles(superUserRoles)
                    .isActive(true)
                    .build();

            userRepository.save(superUser);
            log.info("Created superuser account");
        }
    }
}