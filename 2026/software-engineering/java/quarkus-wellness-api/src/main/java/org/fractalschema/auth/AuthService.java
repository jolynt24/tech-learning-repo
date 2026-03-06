package org.fractalschema.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.fractalschema.config.SecurityConfig;
import org.fractalschema.dto.request.LoginRequest;
import org.fractalschema.dto.request.RegisterRequest;
import org.fractalschema.dto.request.UpdateProfileRequest;
import org.fractalschema.dto.response.AuthResponse;
import org.fractalschema.dto.response.UserResponse;
import org.fractalschema.enums.ErrorCode;
import org.fractalschema.exceptions.CustomExceptions;
import org.fractalschema.util.JwtUtil;
import org.fractalschema.util.PasswordEncoder;

import java.time.Instant;

@ApplicationScoped
public class AuthService {

    @Inject JwtUtil jwtUtil;
    @Inject JsonWebToken jwt;

    @Inject PasswordEncoder passwordEncoder;
    @Inject SecurityConfig securityConfig;

    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if (User.existsByUsername(registerRequest.getUsername()) || User.existsByEmail(registerRequest.getEmail())) {
            throw new CustomExceptions(ErrorCode.USER_EXISTS);
        }
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.hash(registerRequest.getPassword()));

            Instant now = Instant.now();
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setRoles("user");
            user.persist();

            return new UserResponse(user);
        } catch (Exception e) {
            throw new CustomExceptions(
                    ErrorCode.DATABASE_ERROR,
                    "Failed to create a user account",
                    e
            );
        }
    }

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            User user = User.findByEmail(loginRequest.getEmail());
            if (user == null) {
                throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
            }
            if (!passwordEncoder.verify(loginRequest.getPassword(), user.getPassword())) {
                throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
            }

            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRoles());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            return new AuthResponse(accessToken, refreshToken, user.getUsername(), jwtUtil.accessTokenExpiry());
        } catch (CustomExceptions e) {
            throw e;
        } catch (Exception e) {
            throw new CustomExceptions(
                    ErrorCode.LOGIN_ERROR,
                    "Error logging in",
                    e
            );
        }
    }

    public UserResponse me() {
        User user = User.findByUsername(securityConfig.getUserIdentity());
        if (user == null) {
            throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        }
        return new UserResponse(user);
    }

    @Transactional
    public UserResponse profile(UpdateProfileRequest profile) {
        User user = User.findByUsername(securityConfig.getUserIdentity());
        if (user == null) {
            throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        }
        if (profile.getEmail() != null) {
            if (User.existsByEmail(profile.getEmail())) {
                throw new CustomExceptions(ErrorCode.USER_EXISTS);
            }
            user.setEmail(profile.getEmail());
        } else if (profile.getPassword() != null) {
            user.setPassword(passwordEncoder.hash(profile.getPassword()));
        } else {
            throw new CustomExceptions(ErrorCode.BAD_REQUEST);
        }
        user.setUpdatedAt(Instant.now());
        return new UserResponse(user);
    }

    public AuthResponse refresh() {
        String username = securityConfig.getUserIdentity();
        if (! "refresh".equals(jwt.getClaim("type"))) {
            throw new CustomExceptions(ErrorCode.BAD_REQUEST);
        }
        User user = User.findByUsername(username);
        if (user == null) {
            throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        }
        return new AuthResponse(
                jwtUtil.generateAccessToken(username, user.getRoles()),
                jwtUtil.generateRefreshToken(username),
                username,
                jwtUtil.accessTokenExpiry());
    }
}