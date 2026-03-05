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

    @Inject AuthRepository authRepository;

    @Inject JwtUtil jwtUtil;
    @Inject JsonWebToken jwt;

    @Inject PasswordEncoder passwordEncoder;
    @Inject SecurityConfig securityConfig;

    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        if (authRepository.existsByUsername(registerRequest.getUsername()) || authRepository.existsByEmail(registerRequest.getEmail())) {
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
            authRepository.save(user);

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
            User user = authRepository.findByUsername(loginRequest.getUsername());
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
        User user = authRepository.findByUsername(securityConfig.getUserIdentity());
        if (user == null) {
            throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        }
        return new UserResponse(user);
    }

    @Transactional
    public UserResponse profile(UpdateProfileRequest profile) {
        User user = authRepository.findByUsername(securityConfig.getUserIdentity());
        if (user == null) {
            throw new CustomExceptions(ErrorCode.INVALID_CREDENTIALS);
        }
        if (profile.getEmail() != null) {
            if (authRepository.existsByEmail(profile.getEmail())) {
                throw new CustomExceptions(ErrorCode.USER_EXISTS);
            }
            user.setEmail(profile.getEmail());
            user.setUpdatedAt(Instant.now());
        } else if (profile.getPassword() != null) {
            user.setPassword(passwordEncoder.hash(profile.getPassword()));
            user.setUpdatedAt(Instant.now());
        } else {
            throw new CustomExceptions(ErrorCode.BAD_REQUEST);
        }
        return new UserResponse(user);
    }

    public AuthResponse refresh() {
        String username = securityConfig.getUserIdentity();
        if (! "refresh".equals(jwt.getClaim("type"))) {
            throw new CustomExceptions(ErrorCode.BAD_REQUEST);
        }
        User user = authRepository.findByUsername(username);
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