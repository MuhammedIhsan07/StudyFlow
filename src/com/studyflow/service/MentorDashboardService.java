package com.studyflow.service;

import com.studyflow.model.StudentProgressSummary;
import com.studyflow.model.StudentProgressSummary.SubjectProgress;
import com.studyflow.model.StudyTask;
import com.studyflow.model.TaskStatus;
import com.studyflow.model.UserAccount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Builds the mentor dashboard from real student accounts and their private planners. */
public final class MentorDashboardService {
    private final AccountManagementService accountService;

    public MentorDashboardService(AccountManagementService accountService) {
        this.accountService = accountService;
    }

    public List<StudentProgressSummary> getStudents() {
        return accountService.getStudents().stream()
                .map(this::studentSummary)
                .collect(Collectors.toList());
    }

    public int getAverageProgress() {
        return (int) Math.round(getStudents().stream()
                .mapToInt(StudentProgressSummary::getOverallProgress).average().orElse(0));
    }

    public long getOnTrackCount() {
        return getStudents().stream().filter(student -> student.getOverallProgress() >= 75).count();
    }

    public long getNeedsAttentionCount() {
        return getStudents().stream().filter(student -> student.getOverallProgress() < 50).count();
    }

    private StudentProgressSummary studentSummary(UserAccount account) {
        boolean isDemoAccount = "student@studyflow.com".equals(account.getEmail());
        AdaptivePlannerService planner = new AdaptivePlannerService(account.getId(), account.getName(),
                account.getEmail(), account.getProgram(), isDemoAccount);
        List<SubjectProgress> subjects = planner.getSubjects().stream()
                .map(subject -> new SubjectProgress(subject.getName(), subject.getCode(),
                        subject.getColorHex(), subject.getProgressPercent()))
                .collect(Collectors.toList());
        List<String> activity = planner.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .sorted(Comparator.comparing(StudyTask::getDueDateTime).reversed())
                .limit(3)
                .map(task -> "Completed " + task.getTitle())
                .collect(Collectors.toList());
        if (activity.isEmpty()) activity = new ArrayList<>();
        if (activity.isEmpty()) activity.add("No completed tasks recorded yet");

        return new StudentProgressSummary(shortId(account.getId()), account.getName(),
                account.getEmail(), account.getProgram(), planner.getOverallProgress(),
                planner.getCompletedCount(), planner.getWeekCompletedMinutes(), subjects, activity);
    }

    private static String shortId(String id) {
        String compact = id == null ? "NEW" : id.replace("-", "").toUpperCase();
        return "ST-" + compact.substring(0, Math.min(6, compact.length()));
    }
}
