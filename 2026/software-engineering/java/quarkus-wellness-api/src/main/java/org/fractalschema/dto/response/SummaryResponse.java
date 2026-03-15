package org.fractalschema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.fractalschema.enums.Period;

import java.time.LocalDate;
import java.util.List;

@Getter @Builder @AllArgsConstructor
public class SummaryResponse {
    private Period period;
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalEntries;
    private double avgSleepHours;
    private int avgWaterMl;
    private long workoutDays;
    private long totalWorkoutDuration;
    private long totalReadingMinutes;
    private long totalHobbyMinutes;
    private double avgMoodRating;
    private List<StreakResponse> streaks;
}
