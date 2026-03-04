package org.fractalschema.dto.response;

import java.time.Instant;

public record AuthResponse(String token, String refreshToken, String user, Instant expiresIn) {}
