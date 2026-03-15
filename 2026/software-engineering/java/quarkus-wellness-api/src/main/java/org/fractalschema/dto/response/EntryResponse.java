package org.fractalschema.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.fractalschema.entries.DailyEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EntryResponse {
    private Long id;
    private Long userId;
    private LocalDate entryDate;
    private Double sleepHours;
    private Integer sleepQuality;
    private Integer waterMl;
    private Boolean workoutDone;
    private String workoutType;
    private Integer workoutDurationMin;
    private Integer readingMinutes;
    private Integer readingPages;
    private String readingBook;
    private String hobbyActivity;
    private Integer hobbyDurationMin;
    private Integer moodRating;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MealResponse> meals;

    public static EntryResponse from(DailyEntry entry) {
        return EntryResponse.builder().id(entry.id)
                .userId(entry.getUser().id)
                .entryDate(entry.getEntryDate())
                .sleepHours(entry.getSleepHours())
                .sleepQuality(entry.getSleepQuality())
                .waterMl(entry.getWaterMl())
                .workoutDone(entry.getWorkoutDone())
                .workoutType(entry.getWorkoutType())
                .workoutDurationMin(entry.getWorkoutDurationMin())
                .readingMinutes(entry.getReadingMinutes())
                .readingPages(entry.getReadingPages())
                .readingBook(entry.getReadingBook())
                .hobbyActivity(entry.getHobbyActivity())
                .hobbyDurationMin(entry.getHobbyDurationMin())
                .moodRating(entry.getMoodRating())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .meals(entry.getMeals().stream().map(MealResponse::from).toList())
                .build();
    }
}
