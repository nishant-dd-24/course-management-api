package com.nishant.coursemanagement.service.user;


import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.*;

public interface UserService {
    UserResponse createUser(UserRequest request);

    PageResponse<UserResponse> getAllUsers(UserSearchRequest request);

    UserResponse getUserById(Long id);

    UserResponse updateUser(UserAdminUpdateRequest request, Long id);

    UserResponse patchUser(UserAdminPatchRequest request, Long id);

    void deactivateUser(Long id);

    UserResponse getMe();

    UserResponse updateMe(UserSelfUpdateRequest request);

    UserResponse patchMe(UserSelfPatchRequest request);

    void deactivateMe();

    PasswordChangeResponse changePassword(NewPasswordRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(String refreshToken);

    void logout(String token);

    void logout(String token, String refreshToken);
}
