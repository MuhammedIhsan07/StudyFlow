package com.studyflow.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/** Persisted application account. Password material is stored only as salt and hash. */
public final class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final String email;
    private final String program;
    private final UserRole role;
    private final byte[] passwordSalt;
    private final byte[] passwordHash;
    private final LocalDateTime createdAt;
    private boolean active;

    public UserAccount(String name, String email, String program, UserRole role,
                       byte[] passwordSalt, byte[] passwordHash) {
        this(UUID.randomUUID().toString(), name, email, program, role,
                passwordSalt, passwordHash, LocalDateTime.now(), true);
    }

    public UserAccount(String id, String name, String email, String program, UserRole role,
                       byte[] passwordSalt, byte[] passwordHash,
                       LocalDateTime createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.program = program;
        this.role = role;
        this.passwordSalt = Arrays.copyOf(passwordSalt, passwordSalt.length);
        this.passwordHash = Arrays.copyOf(passwordHash, passwordHash.length);
        this.createdAt = createdAt;
        this.active = active;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProgram() { return program; }
    public UserRole getRole() { return role; }
    public byte[] getPasswordSalt() { return Arrays.copyOf(passwordSalt, passwordSalt.length); }
    public byte[] getPasswordHash() { return Arrays.copyOf(passwordHash, passwordHash.length); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, name, email, program, role);
    }
}
