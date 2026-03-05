package org.fractalschema.util;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

@ApplicationScoped
public class JwtUtil {

    private static final String ISSUER = "fractalschema";
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 3600L;       // 1 hour
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 604800L;    // 7 days

    public String generateAccessToken(String username, String roles) {
        return Jwt.issuer(ISSUER)
                .upn(username)
                .groups(roles)
                .claim("type", "access")
                .expiresIn(ACCESS_TOKEN_EXPIRY_SECONDS)
                .sign();
    }

    public String generateRefreshToken(String username) {
        return Jwt.issuer(ISSUER)
                .upn(username)
                .groups("user")
                .claim("type", "refresh")
                .expiresIn(REFRESH_TOKEN_EXPIRY_SECONDS)
                .sign();
    }

    public Instant accessTokenExpiry() {
        return Instant.now().plusSeconds(ACCESS_TOKEN_EXPIRY_SECONDS);
    }
}