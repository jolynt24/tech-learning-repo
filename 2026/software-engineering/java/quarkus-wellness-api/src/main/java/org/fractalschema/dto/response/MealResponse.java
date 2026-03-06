package org.fractalschema.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.fractalschema.entries.Meal;
import org.fractalschema.enums.MealType;

import java.time.Instant;

@Getter
@Builder
public class MealResponse {

    private Long id;
    private MealType mealType;
    private String description;
    private Integer calories;
    private Instant loggedAt;

    public static MealResponse from(Meal meal) {
        return MealResponse.builder().id(meal.id)
                .mealType(meal.getMealType())
                .description(meal.getDescription())
                .calories(meal.getCalories())
                .loggedAt(meal.getLoggedAt())
                .build();

    }
}
