package com.nishant.coursemanagement.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record NewPasswordRequest(
        @NotBlank(message = "Password is required")
        @Size(min=8, message = "Password must be at least 8 characters")
        String oldPassword,

        @NotBlank(message = "Password is required")
        @Size(min=8, message = "Password must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "Password is required")
        @Size(min=8, message = "Password must be at least 8 characters")
        @NotBlank
        String confirmPassword
) {
    @AssertTrue(message = "Passwords must match")
    public boolean isPasswordMatching() {
        return newPassword.equals(confirmPassword);
    }
}
