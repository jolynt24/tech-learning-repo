package org.fractalschema.exceptions;

import org.fractalschema.enums.ErrorCode;

public record ErrorResponse(int code, String message) {
    public ErrorResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }
}