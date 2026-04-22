package com.nishant.coursemanagement.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Refresh-token request payload")
public record RefreshRequest(
        @Schema(description = "Refresh token issued at login", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}

