package com.nishant.coursemanagement.dto.user;

import com.nishant.coursemanagement.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Admin full-update request for a user")
public record UserAdminUpdateRequest(

        @Schema(description = "Updated full name", example = "John Doe")
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Schema(description = "Updated email address", example = "john.doe@example.com")
        @Size(max = 100, message = "Email must be at most 100 characters")
        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @Schema(description = "Assigned role", example = "INSTRUCTOR")
        @NotNull(message = "Role is required")
        Role role,

        @Schema(description = "Whether the user account is active", example = "true")
        @NotNull(message = "Active status is required")
        Boolean isActive
) {
}
