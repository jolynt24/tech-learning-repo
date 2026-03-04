package org.fractalschema.dto.response;

import org.fractalschema.auth.User;

import java.time.Instant;

public record UserResponse(Long id, String username, String email, Instant createdAt) {

    public UserResponse(User user) {
        this(user.id, user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
