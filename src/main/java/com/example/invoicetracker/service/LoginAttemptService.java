package com.example.invoicetracker.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 5;
    private final long LOCK_TIME_MINUTES = 5;

    private static class Attempt {
        int count;
        LocalDateTime firstAttemptTime;

        Attempt() {
            count = 1;
            firstAttemptTime = LocalDateTime.now();
        }
    }

    private final Map<String, Attempt> attempts = new HashMap<>();

    public boolean isBlocked(String username) {
        Attempt attempt = attempts.get(username);
        if (attempt == null)
            return false;

        if (attempt.count >= MAX_ATTEMPTS) {
            if (attempt.firstAttemptTime.plusMinutes(LOCK_TIME_MINUTES).isAfter(LocalDateTime.now())) {
                return true;
            } else {
                attempts.remove(username);
                return false;
            }
        }
        return false;
    }

    public void loginFailed(String username) {
        Attempt attempt = attempts.get(username);
        if (attempt == null) {
            attempts.put(username, new Attempt());
        } else {
            attempt.count++;
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
    }
}
