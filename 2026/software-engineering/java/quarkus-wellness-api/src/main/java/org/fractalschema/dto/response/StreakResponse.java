package org.fractalschema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.fractalschema.enums.GoalType;

import java.time.Instant;

@Getter @Builder
@AllArgsConstructor
public class StreakResponse {
    private GoalType goalType;
    private long longestStreak;
    private long currentStreak;
    private boolean activeToday;
    private Instant calculatedAt;
}
