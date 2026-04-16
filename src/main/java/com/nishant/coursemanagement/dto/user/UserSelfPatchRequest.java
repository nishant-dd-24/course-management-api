package com.nishant.coursemanagement.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record UserSelfPatchRequest(
        String name,

        @Email(message = "Invalid email format")
        String email
) {
}
