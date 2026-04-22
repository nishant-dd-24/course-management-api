package com.nishant.coursemanagement.dto.user;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Login request payload")
public record LoginRequest(

        @Schema(description = "User email", example = "john.doe@example.com")
        @Size(max = 100, message = "Email must be at most 100 characters")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "User password", example = "StrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "The password must be 8 characters in length")
        String password) {
}
