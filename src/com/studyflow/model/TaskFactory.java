package com.studyflow.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Centralized creation hides concrete task types from the UI. */
public final class TaskFactory {
    private TaskFactory() { }

    public static StudyTask create(TaskType type, String title, String subjectId,
                                   LocalDate date, LocalTime time, int duration,
                                   TaskPriority priority, boolean adaptive) {
        switch (type) {
            case ASSIGNMENT:
                return new AssignmentTask(title, subjectId, date, time, duration, priority, adaptive);
            case REVISION:
                return new RevisionTask(title, subjectId, date, time, duration, priority, adaptive);
            case EXAM:
                return new ExamTask(title, subjectId, date, time, duration, priority, adaptive);
            default:
                return new StandardTask(title, subjectId, date, time, duration, priority, adaptive);
        }
    }
}
