package org.fractalschema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.fractalschema.enums.GoalType;
import org.fractalschema.enums.TrendDirection;

import java.util.List;

@Getter @Builder @AllArgsConstructor
public class TrendResponse {
    private GoalType metric;
    private long period;
    private double average;
    private double min;
    private double max;
    private TrendDirection trendDirection;
    private List<DataPoint> dataPoints;
}
