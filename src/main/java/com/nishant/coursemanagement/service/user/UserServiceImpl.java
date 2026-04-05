package com.nishant.coursemanagement.service.user;

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
import com.nishant.coursemanagement.util.LogUtil;
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
    public UserResponse createUser(UserRequest request) {
        request = Sanitizer.sanitizeUserRequest(request);
        try {
            LogUtil.put("action", "CREATE_USER");
            LogUtil.put("email", request.email());
            log.info("Creating user");
        } finally {
            LogUtil.clear();
        }
        if (userRepository.existsByEmail(request.email())) {
            try {
                LogUtil.put("action", "CREATE_USER_DUPLICATE");
                LogUtil.put("email", request.email());
                log.warn("Duplicate user creation attempt");
            } finally {
                LogUtil.clear();
            }
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> exceptionUtil.notFound("User not found"));
            if (!user.getIsActive()) {
                try {
                    LogUtil.put("action", "REACTIVATE_USER");
                    LogUtil.put("email", request.email());
                    log.info("Reactivating user");
                } finally {
                    LogUtil.clear();
                }
                return reactivateUser(user);
            }
            try {
                LogUtil.put("action", "CREATE_USER_FAILED");
                LogUtil.put("reason", "EMAIL_EXISTS");
                LogUtil.put("email", request.email());
                log.warn("User creation failed");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.duplicate("User already exists");
        }
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        try {
            LogUtil.put("action", "CREATE_USER_SUCCESS");
            LogUtil.put("userId", saved.getId());
            LogUtil.put("email", saved.getEmail());
            log.info("User created successfully");
        } finally {
            LogUtil.clear();
        }
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse getUserById(Long id) {
        try {
            LogUtil.put("action", "GET_USER");
            LogUtil.put("userId", id);
            log.debug("Getting user by ID");
        } finally {
            LogUtil.clear();
        }
        return userQueryService.getUserResponse(id);
    }

    @Override
    public UserResponse getMe() {
        return UserMapper.toResponse(authUtil.getCurrentUser());
    }

    @Override
    public PasswordChangeResponse changePassword(NewPasswordRequest request) {
        if (!request.isPasswordMatching()) {
            throw exceptionUtil.badRequest("Passwords do not match");
        }
        User user = authUtil.getCurrentUser();
        try {
            LogUtil.put("action", "CHANGE_PASSWORD");
            LogUtil.put("userId", user.getId());
            log.info("Changing password");
        } finally {
            LogUtil.clear();
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            try {
                LogUtil.put("action", "CHANGE_PASSWORD_FAILED");
                LogUtil.put("reason", "INVALID_OLD_PASSWORD");
                LogUtil.put("userId", user.getId());
                log.warn("Password change failed");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.accessDenied("Invalid current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        User saved = userRepository.save(user);
        try {
            LogUtil.put("action", "CHANGE_PASSWORD_SUCCESS");
            LogUtil.put("userId", user.getId());
            log.info("Password changed successfully");
        } finally {
            LogUtil.clear();
        }
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return new PasswordChangeResponse("Password successfully changed");
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable) {
        try {
            LogUtil.put("action", "GET_ALL_USERS");
            LogUtil.put("name", name);
            LogUtil.put("email", email);
            LogUtil.put("active", active);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Getting all users");
        } finally {
            LogUtil.clear();
        }
        name = StringUtil.makeQueryLike(name);
        email = StringUtil.makeQueryLike(email);
        return userQueryService.getAllUsers(name, email, active, pageable);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userQueryService.getUser(id);
        try {
            LogUtil.put("action", "DELETE_USER");
            LogUtil.put("userId", id);
            LogUtil.put("performedBy", authUtil.getCurrentUser().getId());
            log.warn("Deleting user");
        } finally {
            LogUtil.clear();
        }
        user.setIsActive(false);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
    }

    @Override
    public void deleteMe() {
        deleteUser(authUtil.getCurrentUser().getId());
    }

    @Override
    public UserResponse updateUser(UserUpdateRequest request, Long id) {
        User user = userQueryService.getUser(id);
        request = Sanitizer.sanitizeUserUpdateRequest(request);
        try {
            LogUtil.put("action", "UPDATE_USER");
            LogUtil.put("userId", id);
            log.info("Updating user");
        } finally {
            LogUtil.clear();
        }
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            try {
                LogUtil.put("action", "UPDATE_USER_FAILED");
                LogUtil.put("reason", "EMAIL_EXISTS");
                LogUtil.put("userId", id);
                LogUtil.put("email", request.email());
                log.warn("User update failed");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.duplicate("Email already exists");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        try {
            LogUtil.put("action", "UPDATE_USER_SUCCESS");
            LogUtil.put("userId", id);
            log.info("User updated successfully");
        } finally {
            LogUtil.clear();
        }
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse updateMe(UserUpdateRequest request) {
        return updateUser(request, authUtil.getCurrentUser().getId());
    }

    private void applyPatch(User user, UserPatchRequest request) {
        try {
            LogUtil.put("action", "PATCH_USER_PAYLOAD");
            LogUtil.put("userId", user.getId());
            LogUtil.put("name", request.name());
            LogUtil.put("email", request.email());
            log.debug("Applying patch to user");
        } finally {
            LogUtil.clear();
        }
        Optional.ofNullable(request.name())
                .map(StringUtil::normalize)
                .ifPresent(user::setName);
        Optional.ofNullable(request.email())
                .map(StringUtil::normalize)

                .ifPresent(email -> {
                    if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
                        try {
                            LogUtil.put("action", "PATCH_USER_FAILED");
                            LogUtil.put("reason", "EMAIL_EXISTS");
                            LogUtil.put("userId", user.getId());
                            LogUtil.put("email", email);
                            log.warn("Patch user failed");
                        } finally {
                            LogUtil.clear();
                        }
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
    public UserResponse patchUser(UserPatchRequest request, Long id) {
        validatePatchRequest(request);
        try {
            LogUtil.put("action", "PATCH_USER");
            LogUtil.put("userId", id);
            log.info("Patching user");
        } finally {
            LogUtil.clear();
        }
        User user = userQueryService.getUser(id);
        applyPatch(user, request);
        if (request.role() != null && user.getRole() != request.role()) {
            try {
                LogUtil.put("action", "PATCH_USER_ROLE_CHANGE");
                LogUtil.put("userId", id);
                LogUtil.put("oldRole", user.getRole());
                LogUtil.put("newRole", request.role());
                log.warn("Patching user role change");
            } finally {
                LogUtil.clear();
            }
            user.setRole(request.role());
        }
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    public UserResponse patchMe(UserPatchRequest request) {
        validatePatchRequest(request);
        try {
            LogUtil.put("action", "PATCH_ME");
            LogUtil.put("userId", authUtil.getCurrentUser().getId());
            log.info("Patching me");
        } finally {
            LogUtil.clear();
        }
        User user = authUtil.getCurrentUser();
        applyPatch(user, request);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        request = Sanitizer.sanitizeLoginRequest(request);
        User user = userQueryService.getUser(request.email());
        try {
            LogUtil.put("action", "LOGIN_ATTEMPT");
            LogUtil.put("userId", user.getId());
            log.info("Login attempt");
        } finally {
            LogUtil.clear();
        }
        if (!user.getIsActive()) {
            try {
                LogUtil.put("action", "LOGIN_FAILED");
                LogUtil.put("reason", "INACTIVE_USER");
                LogUtil.put("userId", user.getId());
                log.warn("Login failed");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            try {
                LogUtil.put("action", "LOGIN_FAILED");
                LogUtil.put("reason", "INVALID_PASSWORD");
                LogUtil.put("userId", user.getId());
                log.warn("Login failed");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        try {
            LogUtil.put("action", "LOGIN_SUCCESS");
            LogUtil.put("userId", user.getId());
            log.info("Login successful");
        } finally {
            LogUtil.clear();
        }
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
