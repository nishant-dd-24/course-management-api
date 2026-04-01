package com.nishant.coursemanagement.mapper;



import com.nishant.coursemanagement.dto.user.UserRequest;
import com.nishant.coursemanagement.dto.user.UserResponse;
import com.nishant.coursemanagement.entity.User;

public class UserMapper {
    public static User toEntity(UserRequest request){
        return User.builder()
                .name(request.name())
                .email(request.email())
                .password(request.password())
                .role(request.role())
                .build();
    }

    public static UserResponse toResponse(User user){
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
