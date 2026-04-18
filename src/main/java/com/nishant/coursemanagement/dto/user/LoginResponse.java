package com.nishant.coursemanagement.dto.user;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Instant expiresAt,
        UserResponse user
) {
}