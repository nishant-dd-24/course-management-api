package com.nishant.coursemanagement.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}

