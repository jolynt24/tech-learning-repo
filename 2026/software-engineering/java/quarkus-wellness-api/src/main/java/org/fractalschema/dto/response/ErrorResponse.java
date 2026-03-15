package org.fractalschema.dto.response;

import org.fractalschema.enums.ErrorCode;

import java.time.Instant;

public record ErrorResponse(int code, String message, Instant timestamp) {
    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), Instant.now());
    }
}