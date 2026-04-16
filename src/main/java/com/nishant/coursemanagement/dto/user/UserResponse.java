package com.nishant.coursemanagement.dto.user;


import lombok.Builder;

@Builder
public record UserResponse(Long id, String name, String email, String role, Boolean isActive) {
}
