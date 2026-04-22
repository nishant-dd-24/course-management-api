package com.nishant.coursemanagement.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Self partial-update request for the authenticated user")
public record UserSelfPatchRequest(
        @Schema(description = "Updated full name (optional)", example = "John Doe")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Schema(description = "Updated email address (optional)", example = "john.doe@example.com")
        @Size(max = 100, message = "Email must be at most 100 characters")
        @Email(message = "Invalid email format")
        String email
) {
}
