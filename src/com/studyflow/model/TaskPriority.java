package com.studyflow.model;

public enum TaskPriority {
    LOW("Low", 1), MEDIUM("Medium", 2), HIGH("High", 3);
    private final String label;
    private final int weight;
    TaskPriority(String label, int weight) { this.label = label; this.weight = weight; }
    public int getWeight() { return weight; }
    @Override public String toString() { return label; }
}
