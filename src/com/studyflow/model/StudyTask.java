package com.studyflow.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract task model. Subclasses provide task-specific urgency and adaptive
 * recommendations, demonstrating inheritance and polymorphism.
 */
public abstract class StudyTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String title;
    private final String subjectId;
    private LocalDate dueDate;
    private LocalTime startTime;
    private int durationMinutes;
    private TaskPriority priority;
    private TaskStatus status;
    private boolean adaptive;
    private LocalDate completedDate;

    protected StudyTask(String title, String subjectId, LocalDate dueDate,
                        LocalTime startTime, int durationMinutes,
                        TaskPriority priority, boolean adaptive) {
        this(UUID.randomUUID().toString(), title, subjectId, dueDate, startTime,
                durationMinutes, priority, TaskStatus.SCHEDULED, adaptive, null);
    }

    protected StudyTask(String id, String title, String subjectId, LocalDate dueDate,
                        LocalTime startTime, int durationMinutes, TaskPriority priority,
                        TaskStatus status, boolean adaptive, LocalDate completedDate) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.subjectId = Objects.requireNonNull(subjectId);
        this.dueDate = Objects.requireNonNull(dueDate);
        this.startTime = Objects.requireNonNull(startTime);
        this.durationMinutes = Math.max(15, durationMinutes);
        this.priority = Objects.requireNonNull(priority);
        this.status = Objects.requireNonNull(status);
        this.adaptive = adaptive;
        this.completedDate = completedDate;
    }

    public abstract TaskType getType();
    protected abstract int getTypeUrgencyBoost();
    public abstract String getAdaptiveRecommendation();

    public int getUrgencyScore(LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, dueDate);
        int deadlineScore = days < 0 ? 6 : days == 0 ? 5 : days <= 2 ? 3 : days <= 7 ? 1 : 0;
        return priority.getWeight() * 2 + deadlineScore + getTypeUrgencyBoost();
    }

    public boolean isOverdue(LocalDate today) {
        return status != TaskStatus.COMPLETED && dueDate.isBefore(today);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubjectId() { return subjectId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int minutes) { durationMinutes = Math.max(15, minutes); }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public boolean isAdaptive() { return adaptive; }
    public void setAdaptive(boolean adaptive) { this.adaptive = adaptive; }
    public LocalDate getCompletedDate() { return completedDate; }
    public LocalDateTime getDueDateTime() { return LocalDateTime.of(dueDate, startTime); }

    public void markCompleted(LocalDate date) {
        status = TaskStatus.COMPLETED;
        completedDate = date;
    }

    public void reopen() {
        status = TaskStatus.SCHEDULED;
        completedDate = null;
    }
}
