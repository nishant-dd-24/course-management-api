package com.nishant.coursemanagement.dto.user;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LoginResponse(
        String token,
        long expiresIn,
        Instant expiresAt,
        UserResponse user
) {
}