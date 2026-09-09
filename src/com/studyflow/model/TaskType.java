package com.studyflow.model;

public enum TaskType {
    STUDY("Study session"), ASSIGNMENT("Assignment"),
    REVISION("Revision"), EXAM("Exam preparation");
    private final String label;
    TaskType(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
