package org.fractalschema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class UpdateEntryRequest {

    @DecimalMin("0.0") @DecimalMax("24.0")
    private Double sleepHours;

    @Min(1) @Max(5)
    private Integer sleepQuality;

    @Min(0)
    private Integer waterMl;

    private Boolean workoutDone;
    private String workoutType;

    @Min(0)
    private Integer workoutDurationMin;

    @Min(0)
    private Integer readingMinutes;

    @Min(0)
    private Integer readingPages;

    private String readingBook;
    private String hobbyActivity;

    @Min(0)
    private Integer hobbyDurationMin;

    @Min(1) @Max(5)
    private Integer moodRating;

    private String notes;

    private List<@Valid MealRequest> meals = new ArrayList<>();
}
