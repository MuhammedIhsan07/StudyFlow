package com.studyflow.service;

import com.studyflow.model.StudentProfile;
import com.studyflow.model.StudyTask;
import com.studyflow.model.Subject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Encapsulates local persistence so the rest of the application stays storage-agnostic. */
public final class PlannerRepository {
    private final Path dataFile;

    public PlannerRepository() {
        dataFile = Paths.get(System.getProperty("user.home"), ".studyflow", "planner-data.bin");
    }

    public Optional<PlannerData> load() {
        if (!Files.exists(dataFile)) return Optional.empty();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(dataFile))) {
            Object value = input.readObject();
            return value instanceof PlannerData ? Optional.of((PlannerData) value) : Optional.empty();
        } catch (IOException | ClassNotFoundException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void save(PlannerData data) {
        try {
            Files.createDirectories(dataFile.getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                output.writeObject(data);
            }
        } catch (IOException exception) {
            // The interface remains usable even if local persistence is unavailable.
        }
    }

    public static final class PlannerData implements Serializable {
        private static final long serialVersionUID = 1L;
        private final StudentProfile profile;
        private final List<Subject> subjects;
        private final List<StudyTask> tasks;

        public PlannerData(StudentProfile profile, List<Subject> subjects, List<StudyTask> tasks) {
            this.profile = profile;
            this.subjects = new ArrayList<>(subjects);
            this.tasks = new ArrayList<>(tasks);
        }

        public StudentProfile getProfile() { return profile; }
        public List<Subject> getSubjects() { return new ArrayList<>(subjects); }
        public List<StudyTask> getTasks() { return new ArrayList<>(tasks); }
    }
}
