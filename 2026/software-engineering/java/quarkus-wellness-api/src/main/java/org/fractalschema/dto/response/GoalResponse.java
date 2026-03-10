package org.fractalschema.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Builder @Getter
public class GoalResponse {
    private Long id;
    private String goalLabel;
    private String goalMetrics;
    private BigDecimal target;
    private String goalFrequency;
    private int frequencyDays;
    private boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
}
