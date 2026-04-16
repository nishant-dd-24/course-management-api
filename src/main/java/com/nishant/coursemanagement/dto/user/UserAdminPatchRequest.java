package com.nishant.coursemanagement.dto.user;

import com.nishant.coursemanagement.entity.Role;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record UserAdminPatchRequest(

        String name,

        @Email(message = "Invalid email format")
        String email,

        Role role,

        Boolean isActive
) {
}
