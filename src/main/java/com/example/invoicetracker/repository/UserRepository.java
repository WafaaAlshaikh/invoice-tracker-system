    package com.example.invoicetracker.repository;

    import com.example.invoicetracker.model.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

    import java.util.Optional;

    @Repository
    public interface UserRepository extends JpaRepository<User, String> {
        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        Boolean existsByUsername(String username);

        Boolean existsByEmail(String email);

        Boolean existsByUserId(String userId);
        long countByIsActiveTrue();

        Page<User> findAllByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);
    }
