package com.nishant.coursemanagement.dto.user;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "User registration request payload")
public record UserRequest(
        @Schema(description = "Full name", example = "John Doe")
        @Size(max = 50, message = "Name must be at most 50 characters")
        @NotBlank(message = "Name is required")
        String name,

        @Schema(description = "Email address", example = "john.doe@example.com")
        @Size(max = 100, message = "Email must be at most 100 characters")
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Account password (minimum 8 characters)", example = "StrongPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
