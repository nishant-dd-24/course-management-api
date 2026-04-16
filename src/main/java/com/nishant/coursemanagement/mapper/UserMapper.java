package com.nishant.coursemanagement.mapper;


import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.entity.User;

import static com.nishant.coursemanagement.entity.Role.STUDENT;

public class UserMapper {
    public static User toEntity(UserRequest request) {
        return User.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .role(STUDENT) // Default role is STUDENT, can be changed later by an admin
                .build();
    }

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
    }

    public static void updateEntity(User user, UserAdminUpdateRequest request) {
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setIsActive(request.isActive());
    }

    public static void updateEntity(User user, UserSelfUpdateRequest request) {
        user.setName(request.name());
        user.setEmail(request.email());
    }
}
