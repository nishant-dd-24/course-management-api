package com.nishant.coursemanagement.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Password change request payload")
public record NewPasswordRequest(
        @Schema(description = "Current account password", example = "OldStrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String oldPassword,

        @Schema(description = "New account password", example = "NewStrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,

        @Schema(description = "Must match newPassword", example = "NewStrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @NotBlank
        String confirmPassword
) {
    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordMatching() {
        return newPassword.equals(confirmPassword);
    }
}
