package org.fractalschema.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fractalschema.enums.GoalFrequency;
import org.fractalschema.enums.GoalType;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
public class UpdateGoalRequest {
    private GoalType goalType;
    private Double targetValue;
    private GoalFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
}
