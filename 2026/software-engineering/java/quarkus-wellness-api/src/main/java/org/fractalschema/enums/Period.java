package org.fractalschema.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum Period {
    WEEK(7), MONTH(30);
    private final long days;
}
