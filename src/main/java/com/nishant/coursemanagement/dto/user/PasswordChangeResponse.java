package com.nishant.coursemanagement.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Password change confirmation payload")
public record PasswordChangeResponse(
        @Schema(description = "Operation result message", example = "Password updated successfully")
        String confirmMessage
) {}
