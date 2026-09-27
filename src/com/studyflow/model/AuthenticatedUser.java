package com.studyflow.model;

/** Immutable authenticated-session identity. */
public final class AuthenticatedUser {
    private final String id;
    private final String name;
    private final String email;
    private final String program;
    private final UserRole role;

    public AuthenticatedUser(String id, String name, String email, String program, UserRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.program = program;
        this.role = role;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProgram() { return program; }
    public UserRole getRole() { return role; }
}
