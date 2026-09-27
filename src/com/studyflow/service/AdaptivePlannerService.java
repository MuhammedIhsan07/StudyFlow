package com.studyflow.service;

import com.studyflow.model.StudentProfile;
import com.studyflow.model.StudyTask;
import com.studyflow.model.Subject;
import com.studyflow.model.TaskFactory;
import com.studyflow.model.TaskPriority;
import com.studyflow.model.TaskStatus;
import com.studyflow.model.TaskType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application facade for planning operations. The UI asks this class for data
 * and actions without needing to know scheduling or persistence details.
 */
public final class AdaptivePlannerService {
    private final PlannerRepository repository;
    private StudentProfile profile;
    private final List<Subject> subjects = new ArrayList<>();
    private final List<StudyTask> tasks = new ArrayList<>();

    public AdaptivePlannerService() {
        repository = new PlannerRepository();
        loadOrCreate("Muhammed Ihsan", "ihsan@student.edu", "Btech Computer Science", true);
    }

    /**
     * Opens the planner that belongs to one authenticated student. New accounts
     * start clean; the bundled demonstration account receives example content.
     */
    public AdaptivePlannerService(String userId, String name, String email,
                                  String program, boolean seedExamples) {
        repository = new PlannerRepository(userId);
        loadOrCreate(name, email, program, seedExamples);
    }

    private void loadOrCreate(String name, String email, String program, boolean seedExamples) {
        Optional<PlannerRepository.PlannerData> stored = repository.load();
        if (stored.isPresent()) {
            profile = stored.get().getProfile();
            subjects.addAll(stored.get().getSubjects());
            tasks.addAll(stored.get().getTasks());
        } else {
            profile = new StudentProfile(name, email, program, 180, "Evening (5 PM - 9 PM)");
            if (seedExamples) seedExampleAcademicData();
            save();
        }
    }

    public StudentProfile getProfile() { return profile; }
    public List<Subject> getSubjects() { return new ArrayList<>(subjects); }

    public List<StudyTask> getTasks() {
        return tasks.stream().sorted(Comparator.comparing(StudyTask::getDueDateTime))
                .collect(Collectors.toList());
    }

    public Optional<Subject> findSubject(String id) {
        return subjects.stream().filter(subject -> subject.getId().equals(id)).findFirst();
    }

    public Subject getSubject(String id) {
        return findSubject(id).orElseGet(() -> new Subject("Unassigned", "N/A", "#73706A", 1));
    }

    public void addSubject(Subject subject) {
        subjects.add(subject);
        save();
    }

    public void addTask(StudyTask task) {
        tasks.add(task);
        save();
    }

    public void deleteTask(String id) {
        tasks.removeIf(task -> task.getId().equals(id));
        save();
    }

    public void toggleCompleted(String id) {
        findTask(id).ifPresent(task -> {
            Subject subject = getSubject(task.getSubjectId());
            if (task.getStatus() == TaskStatus.COMPLETED) {
                task.reopen();
                subject.addStudiedMinutes(-task.getDurationMinutes());
            } else {
                task.markCompleted(LocalDate.now());
                subject.addStudiedMinutes(task.getDurationMinutes());
            }
            save();
        });
    }

    /** Finds a balanced future day and moves the task there. */
    public LocalDate adaptTask(String id) {
        Optional<StudyTask> match = findTask(id);
        if (!match.isPresent()) return LocalDate.now();
        StudyTask task = match.get();
        LocalDate candidate = LocalDate.now().plusDays(1);
        for (int i = 0; i < 7; i++) {
            LocalDate day = candidate.plusDays(i);
            int scheduledMinutes = tasks.stream()
                    .filter(item -> item.getStatus() == TaskStatus.SCHEDULED)
                    .filter(item -> item.getDueDate().equals(day))
                    .mapToInt(StudyTask::getDurationMinutes).sum();
            if (scheduledMinutes + task.getDurationMinutes() <= profile.getDailyGoalMinutes() + 60) {
                candidate = day;
                break;
            }
        }
        task.setDueDate(candidate);
        task.reopen();
        save();
        return candidate;
    }

    public Optional<StudyTask> getRecommendedTask() {
        LocalDate today = LocalDate.now();
        return tasks.stream().filter(task -> task.getStatus() == TaskStatus.SCHEDULED)
                .max(Comparator.comparingInt(task -> task.getUrgencyScore(today)));
    }

    public List<StudyTask> getTasksForDate(LocalDate date) {
        return getTasks().stream().filter(task -> task.getDueDate().equals(date))
                .collect(Collectors.toList());
    }

    public List<StudyTask> getOpenTasks() {
        return getTasks().stream().filter(task -> task.getStatus() == TaskStatus.SCHEDULED)
                .collect(Collectors.toList());
    }

    public List<StudyTask> getUpcomingTasks(int days) {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(days);
        return getOpenTasks().stream()
                .filter(task -> !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(end))
                .collect(Collectors.toList());
    }

    public int getCompletedCount() {
        return (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED).count();
    }

    public int getTodayPlannedMinutes() {
        return getTasksForDate(LocalDate.now()).stream().mapToInt(StudyTask::getDurationMinutes).sum();
    }

    public int getTodayCompletedMinutes() {
        return getTasksForDate(LocalDate.now()).stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .mapToInt(StudyTask::getDurationMinutes).sum();
    }

    public int getWeekCompletedMinutes() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        return tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .filter(task -> task.getCompletedDate() != null)
                .filter(task -> !task.getCompletedDate().isBefore(monday)
                        && !task.getCompletedDate().isAfter(sunday))
                .mapToInt(StudyTask::getDurationMinutes).sum();
    }

    public Map<LocalDate, Integer> getWeeklyMinutes(LocalDate monday) {
        Map<LocalDate, Integer> values = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            int minutes = tasks.stream()
                    .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                    .filter(task -> day.equals(task.getCompletedDate()))
                    .mapToInt(StudyTask::getDurationMinutes).sum();
            values.put(day, minutes);
        }
        return values;
    }

    public int getOverallProgress() {
        if (subjects.isEmpty()) return 0;
        return (int) Math.round(subjects.stream().mapToInt(Subject::getProgressPercent).average().orElse(0));
    }

    public void updateProfile(String name, String email, String program,
                              int dailyGoalMinutes, String preferredTime) {
        profile.setName(name);
        profile.setEmail(email);
        profile.setProgram(program);
        profile.setDailyGoalMinutes(dailyGoalMinutes);
        profile.setPreferredStudyTime(preferredTime);
        save();
    }

    private Optional<StudyTask> findTask(String id) {
        return tasks.stream().filter(task -> task.getId().equals(id)).findFirst();
    }

    private void save() {
        repository.save(new PlannerRepository.PlannerData(profile, subjects, tasks));
    }

    private void seedExampleAcademicData() {
        Subject oop = new Subject("Object-Oriented Programming", "CS201", "#7A5C43", 300);
        Subject dsa = new Subject("Data Structures", "CS204", "#B8875C", 240);
        Subject math = new Subject("Discrete Mathematics", "MTH202", "#5F846C", 240);
        Subject database = new Subject("Database Systems", "CS208", "#55534E", 180);
        oop.addStudiedMinutes(210);
        dsa.addStudiedMinutes(135);
        math.addStudiedMinutes(90);
        database.addStudiedMinutes(150);
        subjects.add(oop);
        subjects.add(dsa);
        subjects.add(math);
        subjects.add(database);

        LocalDate today = LocalDate.now();
        StudyTask completed = TaskFactory.create(TaskType.STUDY, "Review encapsulation notes",
                oop.getId(), today, LocalTime.of(8, 30), 45, TaskPriority.MEDIUM, true);
        completed.markCompleted(today);
        tasks.add(completed);
        tasks.add(TaskFactory.create(TaskType.REVISION, "Practice polymorphism examples",
                oop.getId(), today, LocalTime.of(11, 0), 60, TaskPriority.HIGH, true));
        tasks.add(TaskFactory.create(TaskType.ASSIGNMENT, "Complete binary tree exercises",
                dsa.getId(), today, LocalTime.of(15, 30), 75, TaskPriority.HIGH, true));
        tasks.add(TaskFactory.create(TaskType.STUDY, "Relations and functions",
                math.getId(), today.plusDays(1), LocalTime.of(9, 0), 60, TaskPriority.MEDIUM, true));
        tasks.add(TaskFactory.create(TaskType.ASSIGNMENT, "Normalize library database",
                database.getId(), today.plusDays(2), LocalTime.of(17, 0), 90, TaskPriority.HIGH, true));
        tasks.add(TaskFactory.create(TaskType.EXAM, "OOP midterm preparation",
                oop.getId(), today.plusDays(4), LocalTime.of(10, 0), 120, TaskPriority.HIGH, true));
        tasks.add(TaskFactory.create(TaskType.REVISION, "Graph traversal recap",
                dsa.getId(), today.plusDays(5), LocalTime.of(16, 0), 60, TaskPriority.MEDIUM, true));
    }
}
