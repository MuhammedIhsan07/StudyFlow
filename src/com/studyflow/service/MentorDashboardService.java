package com.studyflow.service;

import com.studyflow.model.StudentProgressSummary;
import com.studyflow.model.StudentProgressSummary.SubjectProgress;
import com.studyflow.model.StudyTask;
import com.studyflow.model.Subject;
import com.studyflow.model.TaskStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Aggregates student learning data for the Mentor/Admin dashboard. */
public final class MentorDashboardService {
    private final AdaptivePlannerService studentPlanner;

    public MentorDashboardService(AdaptivePlannerService studentPlanner) {
        this.studentPlanner = studentPlanner;
    }

    public List<StudentProgressSummary> getStudents() {
        List<StudentProgressSummary> students = new ArrayList<>();
        students.add(currentStudent());
        students.add(sample("ST-102", "Omar Khan", "omar@student.edu", 78, 9, 420,
                new int[]{84, 72, 76, 80},
                "Completed queue implementation", "Finished 90-minute database session"));
        students.add(sample("ST-103", "Priya Sharma", "priya@student.edu", 46, 4, 180,
                new int[]{58, 39, 42, 45},
                "Submitted OOP assignment", "Missed discrete mathematics revision"));
        students.add(sample("ST-104", "Daniel Joseph", "daniel@student.edu", 88, 15, 540,
                new int[]{92, 86, 82, 91},
                "Completed graph traversal revision", "Reached weekly study goal"));
        return students;
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

    private StudentProgressSummary currentStudent() {
        List<SubjectProgress> subjectProgress = studentPlanner.getSubjects().stream()
                .map(subject -> new SubjectProgress(subject.getName(), subject.getCode(),
                        subject.getColorHex(), subject.getProgressPercent()))
                .collect(Collectors.toList());
        List<String> activity = studentPlanner.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .sorted(Comparator.comparing(StudyTask::getDueDateTime).reversed())
                .limit(3)
                .map(task -> "Completed " + task.getTitle())
                .collect(Collectors.toList());
        if (activity.isEmpty()) activity.add("No completed tasks recorded yet");

        return new StudentProgressSummary("ST-101", studentPlanner.getProfile().getName(),
                studentPlanner.getProfile().getEmail(), studentPlanner.getProfile().getProgram(),
                studentPlanner.getOverallProgress(), studentPlanner.getCompletedCount(),
                studentPlanner.getWeekCompletedMinutes(), subjectProgress, activity);
    }

    private StudentProgressSummary sample(String id, String name, String email, int progress,
                                          int completed, int minutes, int[] values,
                                          String... activities) {
        List<Subject> sourceSubjects = studentPlanner.getSubjects();
        List<SubjectProgress> subjectProgress = new ArrayList<>();
        for (int index = 0; index < sourceSubjects.size() && index < values.length; index++) {
            Subject subject = sourceSubjects.get(index);
            subjectProgress.add(new SubjectProgress(subject.getName(), subject.getCode(),
                    subject.getColorHex(), values[index]));
        }
        return new StudentProgressSummary(id, name, email, "BSc Computer Science",
                progress, completed, minutes, subjectProgress, Arrays.asList(activities));
    }
}
