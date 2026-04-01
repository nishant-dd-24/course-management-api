package com.nishant.coursemanagement.util;

import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.user.LoginRequest;
import com.nishant.coursemanagement.dto.user.UserRequest;
import com.nishant.coursemanagement.dto.user.UserUpdateRequest;

public class Sanitizer {
    public static UserRequest sanitizeUserRequest(UserRequest request) {
        String normalizedName = StringUtil.normalize(request.name());
        String normalizedEmail = StringUtil.normalize(request.email());

        return UserRequest.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .password(request.password())
                .role(request.role())
                .build();
    }


    public static UserUpdateRequest sanitizeUserUpdateRequest(UserUpdateRequest request) {
        String normalizedName = StringUtil.normalize(request.name());
        String normalizedEmail = StringUtil.normalize(request.email());

        return UserUpdateRequest.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .role(request.role())
                .build();
    }

    public static LoginRequest sanitizeLoginRequest(LoginRequest request) {
        String normalizedEmail = StringUtil.normalize(request.email());

        return LoginRequest.builder()
                .email(normalizedEmail)
                .password(request.password())
                .build();
    }

    public static CourseRequest sanitizeCourseRequest(CourseRequest request) {
        String normalizedTitle = StringUtil.normalize(request.title());
        String normalizedDescription = StringUtil.normalize(request.description());

        return CourseRequest.builder()
                .title(normalizedTitle)
                .description(normalizedDescription)
                .build();
    }
}
