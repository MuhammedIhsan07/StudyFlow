package com.studyflow.model;

import java.io.Serializable;

/** Encapsulates personal details and study preferences. */
public final class StudentProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String program;
    private int dailyGoalMinutes;
    private String preferredStudyTime;

    public StudentProfile(String name, String email, String program,
                          int dailyGoalMinutes, String preferredStudyTime) {
        this.name = name;
        this.email = email;
        this.program = program;
        this.dailyGoalMinutes = dailyGoalMinutes;
        this.preferredStudyTime = preferredStudyTime;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }
    public int getDailyGoalMinutes() { return dailyGoalMinutes; }
    public void setDailyGoalMinutes(int minutes) { this.dailyGoalMinutes = Math.max(30, minutes); }
    public String getPreferredStudyTime() { return preferredStudyTime; }
    public void setPreferredStudyTime(String value) { this.preferredStudyTime = value; }

    public String getInitials() {
        String[] parts = name == null ? new String[0] : name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "ST";
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
