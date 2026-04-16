package com.nishant.coursemanagement.util;

import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseUpdateRequest;
import com.nishant.coursemanagement.dto.user.LoginRequest;
import com.nishant.coursemanagement.dto.user.UserRequest;
import com.nishant.coursemanagement.dto.user.UserAdminUpdateRequest;
import com.nishant.coursemanagement.dto.user.UserSelfUpdateRequest;

public class Sanitizer {
    public static UserRequest sanitizeUserRequest(UserRequest request) {
        String normalizedName = StringUtil.normalize(request.name());
        String normalizedEmail = StringUtil.normalize(request.email());

        return UserRequest.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .password(request.password())
                .build();
    }


    public static UserAdminUpdateRequest sanitizeUserUpdateRequest(UserAdminUpdateRequest request) {
        String normalizedName = StringUtil.normalize(request.name());
        String normalizedEmail = StringUtil.normalize(request.email());

        return UserAdminUpdateRequest.builder()
                .name(normalizedName)
                .email(normalizedEmail)
                .role(request.role())
                .isActive(request.isActive())
                .build();
    }

    public static UserSelfUpdateRequest sanitizeUserUpdateRequest(UserSelfUpdateRequest request) {
        String normalizedName = StringUtil.normalize(request.name());
        String normalizedEmail = StringUtil.normalize(request.email());

        return UserSelfUpdateRequest.builder()
                .name(normalizedName)
                .email(normalizedEmail)
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
                .maxSeats(request.maxSeats())
                .build();
    }

    public static CourseUpdateRequest sanitizeCourseUpdateRequest(CourseUpdateRequest request) {
        String normalizedTitle = StringUtil.normalize(request.title());
        String normalizedDescription = StringUtil.normalize(request.description());

        return CourseUpdateRequest.builder()
                .title(normalizedTitle)
                .description(normalizedDescription)
                .maxSeats(request.maxSeats())
                .build();
    }
}
