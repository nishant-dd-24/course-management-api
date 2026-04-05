package com.nishant.coursemanagement.service.user;


import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.*;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(UserRequest request);

    PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse updateUser(UserUpdateRequest request, Long id);

    UserResponse patchUser(UserPatchRequest request, Long id);

    void deleteUser(Long id);

    UserResponse getMe();

    UserResponse updateMe(UserUpdateRequest request);

    UserResponse patchMe(UserPatchRequest request);

    void deleteMe();

    PasswordChangeResponse changePassword(NewPasswordRequest request);

    LoginResponse login(LoginRequest request);
}
