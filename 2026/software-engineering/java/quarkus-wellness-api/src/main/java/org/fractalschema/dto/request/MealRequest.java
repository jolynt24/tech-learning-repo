package org.fractalschema.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.fractalschema.enums.MealType;

@Getter @Setter
public class MealRequest {
    @NotNull
    private MealType mealType;

    @NotNull @Size(max = 255)
    private String description;

    @Min(0)
    private Integer calories;
}
