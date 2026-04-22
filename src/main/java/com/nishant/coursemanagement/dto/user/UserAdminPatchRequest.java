package com.nishant.coursemanagement.dto.user;

import com.nishant.coursemanagement.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Admin partial-update request for a user")
public record UserAdminPatchRequest(

        @Schema(description = "Updated full name (optional)", example = "John Doe")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Schema(description = "Updated email address (optional)", example = "john.doe@example.com")
        @Size(max = 100, message = "Email must be at most 100 characters")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Updated role (optional)", example = "STUDENT")
        Role role,

        @Schema(description = "Updated active status (optional)", example = "false")
        Boolean isActive
) {
}
