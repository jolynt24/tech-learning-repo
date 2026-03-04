package org.fractalschema.exceptions;

import lombok.Getter;
import org.fractalschema.enums.ErrorCode;

@Getter
public class CustomExceptions extends RuntimeException {
    private final ErrorCode errorCode;
    private final String exceptionMessage;

    public CustomExceptions(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.exceptionMessage = null;
    }

    public CustomExceptions(ErrorCode errorCode, String exceptionMessage) {
        super(exceptionMessage);
        this.errorCode = errorCode;
        this.exceptionMessage = exceptionMessage;
    }

    public CustomExceptions(ErrorCode errorCode, String exceptionMessage, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.exceptionMessage = exceptionMessage;
    }

}
