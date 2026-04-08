package com.nishant.coursemanagement.service.user;

import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.user.UserUpdatedEvent;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.UserMapper;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.security.JwtProperties;
import com.nishant.coursemanagement.security.JwtUtil;
import com.nishant.coursemanagement.log.util.LogUtil;
import com.nishant.coursemanagement.util.Sanitizer;
import com.nishant.coursemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;
    private final ApplicationEventPublisher eventPublisher;

    private UserResponse reactivateUser(User user) {
        user.setIsActive(true);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "CREATE_USER",
            extras = {"#request.email"},
            extraKeys = {"email"}
    )
    public UserResponse createUser(UserRequest request) {
        request = Sanitizer.sanitizeUserRequest(request);
        if (userRepository.existsByEmail(request.email())) {
            LogUtil.log(log, WARN, "CREATE_USER_DUPLICATE", "Duplicate user creation attempt","email", request.email());
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> exceptionUtil.notFound("User not found"));
            if (!user.getIsActive()) {
                LogUtil.log(log, INFO, "REACTIVATE_USER", "Reactivating user","email", request.email());
                return reactivateUser(user);
            }
            LogUtil.log(log, WARN, "CREATE_USER_FAILED", "User creation failed","reason", "EMAIL_EXISTS", "email", request.email());
            throw exceptionUtil.duplicate("User already exists");
        }
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        LogUtil.log(log, INFO, "CREATE_USER_SUCCESS", "User created successfully","userId", saved.getId(), "email", saved.getEmail());
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "GET_USER",
            message = "Get user by id",
            level = DEBUG,
            extras = {"#id"},
            extraKeys = {"userId"}
    )
    public UserResponse getUserById(Long id) {
        return userQueryService.getUserResponse(id);
    }

    @Override
    @Loggable(action = "GET_CURRENT_USER", level = DEBUG, includeCurrentUser = true)
    public UserResponse getMe() {
        return UserMapper.toResponse(authUtil.getCurrentUser());
    }

    @Override
    @Loggable(
            action = "CHANGE_PASSWORD",
            message = "Changing password",
            includeCurrentUser = true
    )
    public PasswordChangeResponse changePassword(NewPasswordRequest request) {
        if (!request.isPasswordMatching()) {
            throw exceptionUtil.badRequest("Passwords do not match");
        }
        User user = authUtil.getCurrentUser();
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            LogUtil.log(log, WARN, "CHANGE_PASSWORD_FAILED","Password change failed", "reason", "INVALID_OLD_PASSWORD", "userId", user.getId());
            throw exceptionUtil.accessDenied("Invalid current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        User saved = userRepository.save(user);
        LogUtil.log(log, INFO, "CHANGE_PASSWORD_SUCCESS", "Password changed successfully", "userId", user.getId());
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return new PasswordChangeResponse("Password successfully changed");
    }

    @Override
    @Loggable(
            action = "GET_ALL_USERS",
            message = "Getting all users",
            level = DEBUG,
            extras = {"#name", "#email", "#active", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"name", "email", "active", "pageNumber", "pageSize"}
    )
    public PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable) {
        name = StringUtil.makeQueryLike(name);
        email = StringUtil.makeQueryLike(email);
        return userQueryService.getAllUsers(name, email, active, pageable);
    }

    @Override
    @Loggable(
            action = "DELETE_USER",
            level = WARN,
            includeCurrentUser = true,
            extras = {"#id"},
            extraKeys = {"deletedUserId"}
    )
    public void deleteUser(Long id) {
        User user = userQueryService.getUser(id);
        user.setIsActive(false);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
    }

    @Override
    @Loggable(
            action = "DELETE_CURRENT_USER",
            level = WARN,
            includeCurrentUser = true
    )
    public void deleteMe() {
        deleteUser(authUtil.getCurrentUser().getId());
    }

    @Override
    @Loggable(
            action = "UPDATE_USER",
            includeCurrentUser = true,
            extras = {"#id"},
            extraKeys = {"updatedUserId"}
    )
    public UserResponse updateUser(UserUpdateRequest request, Long id) {
        User user = userQueryService.getUser(id);
        request = Sanitizer.sanitizeUserUpdateRequest(request);
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            LogUtil.log(log, WARN, "UPDATE_USER_FAILED", "User update failed", "reason", "EMAIL_EXISTS", "userId", id, "email", request.email());
            throw exceptionUtil.duplicate("Email already exists");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        if(authUtil.getCurrentUser().getRole() == Role.ADMIN) {
            LogUtil.log(log, WARN, "UPDATE_USER_ROLE_CHANGE", "Updating user -> role change", "userId", id, "oldRole", user.getRole(), "newRole", request.role());
            user.setRole(request.role());
        }
        LogUtil.log(log, INFO, "UPDATE_USER_SUCCESS", "User updated successfully", "userId", id);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "UPDATE_CURRENT_USER",
            includeCurrentUser = true
    )
    public UserResponse updateMe(UserUpdateRequest request) {
        return updateUser(request, authUtil.getCurrentUser().getId());
    }

    @Loggable(
            action = "PATCH_USER_PAYLOAD",
            level = DEBUG,
            message = "Applying patch to user",
            extras = {"#user.getId()", "#request.name()", "#request.email()"},
            extraKeys = {"userId", "name", "email"}
    )
    private void applyPatch(User user, UserPatchRequest request) {
        Optional.ofNullable(request.name())
                .map(StringUtil::normalize)
                .ifPresent(user::setName);
        Optional.ofNullable(request.email())
                .map(StringUtil::normalize)

                .ifPresent(email -> {
                    if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
                        LogUtil.log(log, WARN, "PATCH_USER_FAILED", "Patch user failed", "reason", "EMAIL_EXISTS", "userId", user.getId(), "email", email);
                        throw exceptionUtil.duplicate("Email already exists");
                    }
                    user.setEmail(email);
                });
    }

    private void validatePatchRequest(UserPatchRequest request) {
        if (request.name() == null && request.email() == null) {
            throw exceptionUtil.badRequest("At least one field must be provided for patching");
        }
        if (StringUtil.isBlankButNotNull(request.name())) {
            throw exceptionUtil.badRequest("Name cannot be blank");
        }
        if (StringUtil.isBlankButNotNull(request.email())) {
            throw exceptionUtil.badRequest("Email cannot be blank");
        }
    }

    @Override
    @Loggable(
            action = "PATCH_USER",
            message = "Patching user",
            includeCurrentUser = true,
            extras = {"#id"},
            extraKeys = {"userId"}
    )
    public UserResponse patchUser(UserPatchRequest request, Long id) {
        validatePatchRequest(request);
        User user = userQueryService.getUser(id);
        applyPatch(user, request);
        if (request.role() != null && user.getRole() != request.role()) {
            LogUtil.log(log, WARN, "PATCH_USER_ROLE_CHANGE", "Patching user -> role change", "userId", id, "oldRole", user.getRole(), "newRole", request.role());
            user.setRole(request.role());
        }
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "PATCH_CURRENT_USER",
            includeCurrentUser = true
    )
    public UserResponse patchMe(UserPatchRequest request) {
        validatePatchRequest(request);
        User user = authUtil.getCurrentUser();
        applyPatch(user, request);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "LOGIN",
            extras = {"#request.email()"},
            extraKeys = {"userEmail"}
    )
    public LoginResponse login(LoginRequest request) {
        request = Sanitizer.sanitizeLoginRequest(request);
        User user = userQueryService.getUser(request.email());
        if (!user.getIsActive()) {
            LogUtil.log(log, WARN, "LOGIN_FAILED", "Login failed", "reason", "INACTIVE_USER", "userId", user.getId());
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            LogUtil.log(log, WARN, "LOGIN_FAILED", "Login failed", "reason", "INVALID_PASSWORD", "userId", user.getId());
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        LogUtil.log(log, INFO, "LOGIN_SUCCESS", "Login successful", "userId", user.getId());
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        UserResponse response = UserMapper.toResponse(user);
        long expiresIn = jwtProperties.getExpirationSeconds();
        Instant expiresAt = Instant.now().plusSeconds(expiresIn);
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .expiresAt(expiresAt)
                .user(response)
                .build();
    }
}
