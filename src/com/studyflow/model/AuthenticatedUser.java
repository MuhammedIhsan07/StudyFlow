package com.studyflow.model;

/** Immutable authenticated-session identity. */
public final class AuthenticatedUser {
    private final String name;
    private final String email;
    private final UserRole role;

    public AuthenticatedUser(String name, String email, UserRole role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
}
