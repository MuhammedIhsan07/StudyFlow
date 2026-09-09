package com.studyflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

public final class StandardTask extends StudyTask {
    private static final long serialVersionUID = 1L;

    public StandardTask(String title, String subjectId, LocalDate date, LocalTime time,
                        int duration, TaskPriority priority, boolean adaptive) {
        super(title, subjectId, date, time, duration, priority, adaptive);
    }

    @Override public TaskType getType() { return TaskType.STUDY; }
    @Override protected int getTypeUrgencyBoost() { return 0; }
    @Override public String getAdaptiveRecommendation() {
        return "Move this session to the next balanced study slot.";
    }
}
