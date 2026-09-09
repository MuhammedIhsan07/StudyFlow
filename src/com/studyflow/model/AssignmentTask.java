package com.studyflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class AssignmentTask extends StudyTask {
    private static final long serialVersionUID = 1L;

    public AssignmentTask(String title, String subjectId, LocalDate date, LocalTime time,
                          int duration, TaskPriority priority, boolean adaptive) {
        super(title, subjectId, date, time, duration, priority, adaptive);
    }

    @Override public TaskType getType() { return TaskType.ASSIGNMENT; }
    @Override protected int getTypeUrgencyBoost() { return 2; }
    @Override public String getAdaptiveRecommendation() {
        return "Protect the deadline by moving this assignment to the next free day.";
    }
}
