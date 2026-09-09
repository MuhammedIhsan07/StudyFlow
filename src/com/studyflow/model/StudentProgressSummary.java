package com.studyflow.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only student progress data shown in the Mentor/Admin workspace. */
public final class StudentProgressSummary {
    private final String id;
    private final String name;
    private final String email;
    private final String program;
    private final int overallProgress;
    private final int completedTasks;
    private final int weeklyMinutes;
    private final List<SubjectProgress> subjects;
    private final List<String> recentActivity;

    public StudentProgressSummary(String id, String name, String email, String program,
                                  int overallProgress, int completedTasks, int weeklyMinutes,
                                  List<SubjectProgress> subjects, List<String> recentActivity) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.program = program;
        this.overallProgress = Math.max(0, Math.min(100, overallProgress));
        this.completedTasks = Math.max(0, completedTasks);
        this.weeklyMinutes = Math.max(0, weeklyMinutes);
        this.subjects = Collections.unmodifiableList(new ArrayList<>(subjects));
        this.recentActivity = Collections.unmodifiableList(new ArrayList<>(recentActivity));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProgram() { return program; }
    public int getOverallProgress() { return overallProgress; }
    public int getCompletedTasks() { return completedTasks; }
    public int getWeeklyMinutes() { return weeklyMinutes; }
    public List<SubjectProgress> getSubjects() { return subjects; }
    public List<String> getRecentActivity() { return recentActivity; }

    public String getInitials() {
        String[] parts = name.trim().split("\\s+");
        String first = parts.length == 0 ? "S" : parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    public ProgressStatus getStatus() {
        if (overallProgress >= 75) return ProgressStatus.ON_TRACK;
        if (overallProgress >= 50) return ProgressStatus.STEADY;
        return ProgressStatus.NEEDS_ATTENTION;
    }

    public enum ProgressStatus {
        ON_TRACK("On track"),
        STEADY("Steady"),
        NEEDS_ATTENTION("Needs attention");

        private final String label;
        ProgressStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public static final class SubjectProgress {
        private final String name;
        private final String code;
        private final String colorHex;
        private final int progress;

        public SubjectProgress(String name, String code, String colorHex, int progress) {
            this.name = name;
            this.code = code;
            this.colorHex = colorHex;
            this.progress = Math.max(0, Math.min(100, progress));
        }

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getColorHex() { return colorHex; }
        public int getProgress() { return progress; }
    }
}
