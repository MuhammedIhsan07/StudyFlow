package com.studyflow.model;

/** Defines which application workspace an authenticated user may access. */
public enum UserRole {
    STUDENT("Student"),
    MENTOR("Mentor"),
    ADMIN("Administrator");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
