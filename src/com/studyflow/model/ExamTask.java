package com.studyflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ExamTask extends StudyTask {
    private static final long serialVersionUID = 1L;

    public ExamTask(String title, String subjectId, LocalDate date, LocalTime time,
                    int duration, TaskPriority priority, boolean adaptive) {
        super(title, subjectId, date, time, duration, priority, adaptive);
    }

    @Override public TaskType getType() { return TaskType.EXAM; }
    @Override protected int getTypeUrgencyBoost() { return 4; }
    @Override public String getAdaptiveRecommendation() {
        return "Reschedule within 24 hours and keep this exam-prep block high priority.";
    }
}
