package com.nishant.coursemanagement.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "Authentication response payload")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.access.token")
        String accessToken,
        @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        String refreshToken,
        @Schema(description = "Access token validity in seconds", example = "3600")
        long expiresIn,
        @Schema(description = "Access token expiry timestamp in UTC", example = "2026-04-22T12:00:00Z")
        Instant expiresAt,
        @Schema(description = "Authenticated user details")
        UserResponse user
) {
}