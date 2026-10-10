package com.ufh.ufhresidencemaintenance.util;

/** Small, testable policy for login lockout rules. */
public final class LoginSecurityPolicy {
    public static final int MAX_LOGIN_ATTEMPTS = 5;

    private LoginSecurityPolicy() { }

    public static boolean shouldLock(int failedAttempts) {
        return failedAttempts >= MAX_LOGIN_ATTEMPTS;
    }
}
