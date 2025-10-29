package com.example.invoicetracker.config;

import com.example.invoicetracker.model.entity.Role;
import com.example.invoicetracker.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final RoleRepository roleRepository;

    @PostConstruct
    @Transactional
    public void init() {
        createRoles();
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
}