package org.fractalschema.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter @Builder
@AllArgsConstructor
public class DataPoint {
    private double value;
    private LocalDate time;
}
