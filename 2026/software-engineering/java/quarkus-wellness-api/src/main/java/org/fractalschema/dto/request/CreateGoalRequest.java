package org.fractalschema.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fractalschema.enums.GoalFrequency;
import org.fractalschema.enums.GoalType;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class CreateGoalRequest {

    @NotNull
    private GoalType goalType;

    @NotNull @DecimalMin(value = "0", inclusive = false)
    private Double target;

    @NotNull
    private GoalFrequency goalFrequency;

    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
}
