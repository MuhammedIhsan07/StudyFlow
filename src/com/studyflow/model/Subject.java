package com.studyflow.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** A course subject with a weekly goal and accumulated progress. */
public final class Subject implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private String name;
    private String code;
    private String colorHex;
    private int weeklyGoalMinutes;
    private int studiedMinutes;

    public Subject(String name, String code, String colorHex, int weeklyGoalMinutes) {
        this(UUID.randomUUID().toString(), name, code, colorHex, weeklyGoalMinutes, 0);
    }

    public Subject(String id, String name, String code, String colorHex,
                   int weeklyGoalMinutes, int studiedMinutes) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.code = Objects.requireNonNull(code);
        this.colorHex = Objects.requireNonNull(colorHex);
        this.weeklyGoalMinutes = Math.max(1, weeklyGoalMinutes);
        this.studiedMinutes = Math.max(0, studiedMinutes);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public int getWeeklyGoalMinutes() { return weeklyGoalMinutes; }
    public void setWeeklyGoalMinutes(int minutes) { weeklyGoalMinutes = Math.max(1, minutes); }
    public int getStudiedMinutes() { return studiedMinutes; }
    public void addStudiedMinutes(int minutes) { studiedMinutes = Math.max(0, studiedMinutes + minutes); }
    public int getProgressPercent() {
        return Math.min(100, (int) Math.round(studiedMinutes * 100.0 / weeklyGoalMinutes));
    }

    @Override
    public String toString() { return name; }
}
