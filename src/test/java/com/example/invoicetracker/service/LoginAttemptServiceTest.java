package com.example.invoicetracker.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Nested
    class BasicTests {
        @Test
        void newUser_isNotBlocked() {
            assertFalse(service.isBlocked("user1"));
        }

        @Test
        void loginSucceeded_resetsAttempts() {
            String username = "user1";
            
            // 3 attempts then success
            service.loginFailed(username);
            service.loginFailed(username);
            service.loginFailed(username);
            service.loginSucceeded(username);
            
            // Should be reset
            service.loginFailed(username);
            assertFalse(service.isBlocked(username));
        }
    }

    @Nested
    class BlockingTests {
        @Test
        void userGetsBlockedAfterMaxAttempts() {
            String username = "user1";

            for (int i = 0; i < 5; i++) {
                service.loginFailed(username);
            }

            assertTrue(service.isBlocked(username));
        }

        @Test
        void userRemainsBlockedAfterMoreAttempts() {
            String username = "user1";

            for (int i = 0; i < 10; i++) {
                service.loginFailed(username);
            }

            assertTrue(service.isBlocked(username));
        }
    }

    @Nested
    class BlockExpirationTests {
        @Test
        void blockExpiresAfterLockTime() throws Exception {
            String username = "user1";

            // Block the user
            for (int i = 0; i < 5; i++) {
                service.loginFailed(username);
            }
            assertTrue(service.isBlocked(username));

            // Simulate time passing (6 minutes - more than LOCK_TIME_MINUTES)
            setFirstAttemptTimeToPast(username, 6);

            assertFalse(service.isBlocked(username));
        }

        @Test
        void blockNotExpiredBeforeLockTime() throws Exception {
            String username = "user1";

            // Block the user
            for (int i = 0; i < 5; i++) {
                service.loginFailed(username);
            }
            assertTrue(service.isBlocked(username));

            // Simulate less time passing (4 minutes - less than LOCK_TIME_MINUTES)
            setFirstAttemptTimeToPast(username, 4);

            assertTrue(service.isBlocked(username));
        }
    }

    @Nested
    class MultipleUsersTests {
        @Test
        void usersAreTrackedIndependently() {
            String user1 = "user1";
            String user2 = "user2";

            // User1 gets blocked
            for (int i = 0; i < 5; i++) {
                service.loginFailed(user1);
            }

            // User2 has only 2 attempts
            service.loginFailed(user2);
            service.loginFailed(user2);

            assertTrue(service.isBlocked(user1));
            assertFalse(service.isBlocked(user2));
        }
    }

    @Nested
    class EdgeCasesTests {
        @Test
        void nullAndEmptyUsernames_areHandled() {
            assertFalse(service.isBlocked(null));
            assertFalse(service.isBlocked(""));
            
            // Should not throw exceptions
            service.loginFailed(null);
            service.loginSucceeded(null);
            service.loginFailed("");
            service.loginSucceeded("");
        }
    }

    // Helper method for time manipulation
    private void setFirstAttemptTimeToPast(String username, int minutesAgo) throws Exception {
        Field attemptsField = LoginAttemptService.class.getDeclaredField("attempts");
        attemptsField.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> attemptsMap = (Map<String, Object>) attemptsField.get(service);
        
        Object attemptObj = attemptsMap.get(username);
        if (attemptObj != null) {
            Field firstAttemptTimeField = attemptObj.getClass().getDeclaredField("firstAttemptTime");
            firstAttemptTimeField.setAccessible(true);
            firstAttemptTimeField.set(attemptObj, LocalDateTime.now().minusMinutes(minutesAgo));
        }
    }
}