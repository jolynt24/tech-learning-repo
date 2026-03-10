package org.fractalschema.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Auth Errors
    USER_EXISTS(1001, "User already exists", 409),
    LOGIN_ERROR(1002, "Error logging in", 401),
    INVALID_CREDENTIALS(1003, "Invalid Credentials", 401),

    // Database Errors
    DATABASE_ERROR(2001, "Database operation failed", 500),

    // Requests
    BAD_REQUEST(3001, "Bad Request", 400),

    // Application
    DUPLICATE_ENTRY(4001, "Duplicate entry", 409),
    ENTRY_NOT_FOUND(4002, "Entry not found", 404),
    GOAL_NOT_FOUND(4003, "Goal not found", 404);

    private final int code;
    private final String message;
    private final int statusCode;

    ErrorCode(int code, String message, int statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
