package com.nishant.coursemanagement.dto.user;


import lombok.Builder;

@Builder
public record LoginResponse(String token,
                            UserResponse response) {
}
