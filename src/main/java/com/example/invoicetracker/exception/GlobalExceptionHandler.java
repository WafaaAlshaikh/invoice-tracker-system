package com.example.invoicetracker.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UserDeactivatedException.class)
    public ResponseEntity<?> handleUserDeactivated(UserDeactivatedException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<?> handleTooManyAttempts(TooManyAttemptsException ex) {
        return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<?> handleDuplicateUser(DuplicateUserException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
public ResponseEntity<?> handleRoleNotFound(RoleNotFoundException ex) {
    return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
}

}
