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

    private void verifyAdmin() {
        if (authUtil.getCurrentUser().getRole() != Role.ADMIN) {
            throw exceptionUtil.accessDenied("Admin access required");
        }
    }

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
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> exceptionUtil.notFound("User not found"));
            if (!user.getIsActive()) {
                LogUtil.log(log, INFO, "REACTIVATE_USER", "Reactivating user","email", request.email());
                return reactivateUser(user);
            }
            throw exceptionUtil.duplicate("User already exists");
        }
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        User saved = userRepository.save(user);
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
        verifyAdmin();
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
            throw exceptionUtil.unauthorized("Invalid current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return new PasswordChangeResponse("Password successfully changed");
    }

    @Override
    @Loggable(
            action = "GET_ALL_USERS",
            message = "Getting all users",
            level = DEBUG,
            extras = {"#name", "#email", "#isActive", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"name", "email", "isActive", "pageNumber", "pageSize"}
    )
    public PageResponse<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable) {
        verifyAdmin();
        name = StringUtil.makeQueryLike(name);
        email = StringUtil.makeQueryLike(email);
        return userQueryService.getAllUsers(name, email, active, pageable);
    }

    private void deactivateUser(User user) {
        user.setIsActive(false);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
    }

    @Override
    @Loggable(
            action = "DELETE_USER",
            level = WARN,
            includeCurrentUser = true,
            extras = {"#id"},
            extraKeys = {"deletedUserId"}
    )
    public void deactivateUser(Long id) {
        verifyAdmin();
        User user = userQueryService.getUser(id);
        deactivateUser(user);
    }

    @Override
    @Loggable(
            action = "DELETE_CURRENT_USER",
            level = WARN,
            includeCurrentUser = true
    )
    public void deactivateMe() {
        deactivateUser(authUtil.getCurrentUser());
    }


    private void checkEmailUniqueness(String email, Long currentUserId) {
        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (!existingUser.getId().equals(currentUserId)) {
                throw exceptionUtil.duplicate("Email already exists");
            }
        }
    }

    @Override
    @Loggable(
            action = "UPDATE_USER",
            includeCurrentUser = true,
            extras = {"#id"},
            extraKeys = {"updatedUserId"}
    )
    public UserResponse updateUser(UserAdminUpdateRequest request, Long id) {
        verifyAdmin();
        User user = userQueryService.getUser(id);
        request = Sanitizer.sanitizeUserUpdateRequest(request);
        checkEmailUniqueness(request.email(), id);
        LogUtil.log(log, WARN, "UPDATE_USER_ROLE_CHANGE", "Updating user -> role change", "userId", id, "oldRole", user.getRole(), "newRole", request.role());
        UserMapper.updateEntity(user, request);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "UPDATE_CURRENT_USER",
            includeCurrentUser = true
    )
    public UserResponse updateMe(UserSelfUpdateRequest request) {
        User currentUser = authUtil.getCurrentUser();
        request = Sanitizer.sanitizeUserUpdateRequest(request);
        checkEmailUniqueness(request.email(), currentUser.getId());
        UserMapper.updateEntity(currentUser, request);
        User saved = userRepository.save(currentUser);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    private void patchNameAndEmail(User user, String name, String email) {
        Optional.ofNullable(name)
                .map(StringUtil::normalize)
                .ifPresent(user::setName);
        Optional.ofNullable(email)
                .map(StringUtil::normalize)
                .ifPresent( normalizedEmail -> {
                    checkEmailUniqueness(normalizedEmail, user.getId());
                    user.setEmail(normalizedEmail);
                });
    }

    @Loggable(
            action = "PATCH_USER_PAYLOAD",
            level = DEBUG,
            message = "Applying patch to user",
            extras = {"#user.getId()", "#request.name()", "#request.email()", "#request.role()", "#request.isActive()"},
            extraKeys = {"userId", "name", "email", "role", "isActive"}
    )
    private void applyPatch(User user, UserAdminPatchRequest request) {
        patchNameAndEmail(user, request.name(), request.email());
        Optional.ofNullable(request.role())
                .ifPresent(role -> {
                    if (user.getRole() != role) {
                        LogUtil.log(log, WARN, "PATCH_USER_ROLE_CHANGE", "Patching user -> role change", "userId", user.getId(), "oldRole", user.getRole(), "newRole", role);
                        user.setRole(role);
                    }
                });
        Optional.ofNullable(request.isActive())
                .ifPresent(isActive -> {
                    if (user.getIsActive() != isActive) {
                        LogUtil.log(log, WARN, "PATCH_USER_STATUS_CHANGE", "Patching user -> status change", "userId", user.getId(), "oldStatus", user.getIsActive(), "newStatus", isActive);
                        user.setIsActive(isActive);
                    }
                });
    }



    private void validatePatchRequest(UserAdminPatchRequest request) {
        if (request.name() == null && request.email() == null && request.role() == null && request.isActive() == null) {
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
    public UserResponse patchUser(UserAdminPatchRequest request, Long id) {
        verifyAdmin();
        validatePatchRequest(request);
        User user = userQueryService.getUser(id);
        applyPatch(user, request);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserUpdatedEvent(saved.getId()));
        return UserMapper.toResponse(saved);
    }

    @Loggable(
            action = "PATCH_USER_PAYLOAD",
            level = DEBUG,
            message = "Applying patch to user",
            extras = {"#user.getId()", "#request.name()", "#request.email()"},
            extraKeys = {"userId", "name", "email"}
    )
    private void applyPatch(User user, UserSelfPatchRequest request) {
        patchNameAndEmail(user, request.name(), request.email());
    }

    private void validatePatchRequest(UserSelfPatchRequest request) {
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
            action = "PATCH_CURRENT_USER",
            includeCurrentUser = true
    )
    public UserResponse patchMe(UserSelfPatchRequest request) {
        validatePatchRequest(request);
        User currentUser = authUtil.getCurrentUser();
        applyPatch(currentUser, request);
        User saved = userRepository.save(currentUser);
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
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
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
