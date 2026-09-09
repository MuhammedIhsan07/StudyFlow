package com.studyflow.service;

import com.studyflow.model.AuthenticatedUser;
import com.studyflow.model.UserRole;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Frontend demonstration authentication with isolated role credentials. */
public final class AuthenticationService {
    public static final String STUDENT_EMAIL = "student@studyflow.com";
    public static final String MENTOR_EMAIL = "mentor@studyflow.com";

    public Optional<AuthenticatedUser> authenticate(String email, char[] password, UserRole role) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ENGLISH);
        char[] expectedPassword = role == UserRole.STUDENT
                ? "student123".toCharArray() : "mentor123".toCharArray();
        String expectedEmail = role == UserRole.STUDENT ? STUDENT_EMAIL : MENTOR_EMAIL;
        String displayName = role == UserRole.STUDENT ? "Aisha Malik" : "Dr. Maya Rao";

        try {
            if (expectedEmail.equals(normalizedEmail) && Arrays.equals(expectedPassword, password)) {
                return Optional.of(new AuthenticatedUser(displayName, expectedEmail, role));
            }
            return Optional.empty();
        } finally {
            Arrays.fill(expectedPassword, '\0');
        }
    }
}
