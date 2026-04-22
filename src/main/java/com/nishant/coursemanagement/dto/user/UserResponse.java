package com.nishant.coursemanagement.dto.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "User response payload")
public record UserResponse(
		@Schema(description = "User ID", example = "1") Long id,
		@Schema(description = "Full name", example = "John Doe") String name,
		@Schema(description = "Email address", example = "john.doe@example.com") String email,
		@Schema(description = "Assigned role", example = "STUDENT") String role,
		@Schema(description = "Whether user is active", example = "true") Boolean isActive
) {
}
