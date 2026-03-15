package org.fractalschema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder @AllArgsConstructor
public class GoalProgressResponse {
    private GoalResponse goalResponse;
    private int daysCompleted;
    private double currentValue;
    private double targetValue;
    private double progressPercentage;
    private int daysInPeriod;
    private boolean onTrack;
}
