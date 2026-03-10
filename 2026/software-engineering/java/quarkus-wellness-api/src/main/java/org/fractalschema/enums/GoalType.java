package org.fractalschema.enums;

import lombok.Getter;

@Getter
public enum GoalType {
    SLEEP("sleep", "hours"),
    WATER("water", "ml"),
    WORKOUT("workout", "minutes"),
    READING("reading", "pages"),
    HOBBY("hobby", "minutes");

    private final String label;
    private final String metrics;

    GoalType(String label, String metrics) {
        this.label = label;
        this.metrics = metrics;
    }
}