package com.studyflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class RevisionTask extends StudyTask {
    private static final long serialVersionUID = 1L;

    public RevisionTask(String title, String subjectId, LocalDate date, LocalTime time,
                        int duration, TaskPriority priority, boolean adaptive) {
        super(title, subjectId, date, time, duration, priority, adaptive);
    }

    @Override public TaskType getType() { return TaskType.REVISION; }
    @Override protected int getTypeUrgencyBoost() { return 1; }
    @Override public String getAdaptiveRecommendation() {
        return "Use a shorter revision block tomorrow to keep recall consistent.";
    }
}
