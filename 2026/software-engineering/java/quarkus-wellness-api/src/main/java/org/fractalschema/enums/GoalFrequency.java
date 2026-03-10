package org.fractalschema.enums;

import lombok.Getter;

@Getter
public enum GoalFrequency {
    DAILY("daily", 1), WEEKLY("weekly", 7), MONTHLY("monthly", 30);

    private final String label;
    private final int days;

    GoalFrequency(String label, int days) {
        this.label = label;
        this.days = days;
    }
}
