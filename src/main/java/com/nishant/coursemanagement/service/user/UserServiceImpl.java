package com.nishant.coursemanagement.service.user;

import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.UserMapper;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.security.JwtUtil;
import com.nishant.coursemanagement.util.Sanitizer;
import com.nishant.coursemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;

    private UserResponse reactivateUser(User user){
        user.setIsActive(true);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse createUser(UserRequest request){
        request = Sanitizer.sanitizeUserRequest(request);
        log.info("action=CREATE_USER email={}", request.email());
        if(userRepository.existsByEmail(request.email())) {
            log.warn("action=CREATE_USER_DUPLICATE email={}", request.email());
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> exceptionUtil.notFound("User not found"));
            if(!user.getIsActive()) {
                log.info("action=REACTIVATE_USER email={}", request.email());
                return reactivateUser(user);
            }
            log.warn("action=CREATE_USER_FAILED reason=EMAIL_EXISTS email={}", request.email());
            throw exceptionUtil.duplicate("User already exists");
        }
        User user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);
        log.info("action=CREATE_USER_SUCCESS userId={} email={}", savedUser.getId(), savedUser.getEmail());
        return UserMapper.toResponse(savedUser);
    }

    private User getUser(Long id){
        return userRepository.findById(id).orElseThrow(() -> exceptionUtil.notFound("User not found"));
    }

    @Override
    public UserResponse getUserById(Long id){
        log.debug("action=GET_USER userId={}", id);
        return UserMapper.toResponse(getUser(id));
    }

    @Override
    public UserResponse getMe(){
        return UserMapper.toResponse(authUtil.getCurrentUser());
    }

    @Override
    public PasswordChangeResponse changePassword(NewPasswordRequest request) {
        if (!request.isPasswordMatching()) {
            throw exceptionUtil.badRequest("Passwords do not match");
        }
        User user = authUtil.getCurrentUser();
        log.info("action=CHANGE_PASSWORD userId={}", user.getId());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            log.warn("action=CHANGE_PASSWORD_FAILED reason=INVALID_OLD_PASSWORD userId={}", user.getId());
            throw exceptionUtil.accessDenied("Invalid current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        log.info("action=CHANGE_PASSWORD_SUCCESS userId={}", user.getId());
        return new PasswordChangeResponse("Password successfully changed");
    }

    @Override
    public Page<UserResponse> getAllUsers(String name, String email, Boolean active, Pageable pageable){
        log.debug("action=GET_ALL_USERS name={} email={} active={} pageNumber={} pageSize={}", name, email, active, pageable.getPageNumber(), pageable.getPageSize());
        name = StringUtil.makeQueryLike(name);
        email = StringUtil.makeQueryLike(email);
        return userRepository.findUsers(name, email, active, pageable)
                .map(UserMapper::toResponse);
    }

    @Override
    public void deleteUser(Long id){
        User user = getUser(id);
        log.warn("action=DELETE_USER userId={} performedBy = {} ", id, authUtil.getCurrentUser().getId());
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void deleteMe() {
        deleteUser(authUtil.getCurrentUser().getId());
    }

    @Override
    public UserResponse updateUser(UserUpdateRequest request, Long id){
        User user = getUser(id);
        request = Sanitizer.sanitizeUserUpdateRequest(request);
        log.info("action=UPDATE_USER userId={}", id);
        if(!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())){
            log.warn("action=UPDATE_USER_FAILED reason=EMAIL_EXISTS userId={} email={}", id, request.email());
            throw exceptionUtil.duplicate("Email already exists");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        log.info("action=UPDATE_USER_SUCCESS userId={}", id);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateMe(UserUpdateRequest request) {
        return updateUser(request, authUtil.getCurrentUser().getId());
    }

    private void applyPatch(User user, UserPatchRequest request) {
        log.debug("action=PATCH_USER_PAYLOAD userId={} name={} email={}", user.getId(), request.name(), request.email());
        Optional.ofNullable(request.name())
                .map(StringUtil::normalize)
                .ifPresent(user::setName);
        Optional.ofNullable(request.email())
                .map(StringUtil::normalize)

                .ifPresent(email -> {
            if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)){
                log.warn("action=PATCH_USER_FAILED reason=EMAIL_EXISTS userId={} email={}", user.getId(), email);
                throw exceptionUtil.duplicate("Email already exists");
            }
            user.setEmail(email);
        });
    }

    private void validatePatchRequest(UserPatchRequest request) {
        if (request.name()==null && request.email()==null) {
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
        User user = getUser(id);
        log.info("action=PATCH_USER userId={}", id);
        applyPatch(user, request);
        if (user.getRole() != request.role() && request.role() != null) {
            log.warn("action=PATCH_USER_ROLE_CHANGE userId={} oldRole={} newRole={}", id, user.getRole(), request.role());
            user.setRole(request.role());
        }
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse patchMe(UserPatchRequest request) {
        validatePatchRequest(request);
        log.info("action=PATCH_ME userId={}", authUtil.getCurrentUser().getId());
        User user = authUtil.getCurrentUser();
        applyPatch(user, request);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public LoginResponse login(LoginRequest request){
        request = Sanitizer.sanitizeLoginRequest(request);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> exceptionUtil.unauthorized("Invalid credentials"));
        log.info("action=LOGIN_ATTEMPT userId={}", user.getId());
        if(!user.getIsActive()){
            log.warn("action=LOGIN_FAILED reason=INACTIVE_USER userId={}", user.getId());
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            log.warn("action=LOGIN_FAILED reason=INVALID_PASSWORD userId={}", user.getId());
            throw exceptionUtil.unauthorized("Invalid credentials");
        }
        log.info("action=LOGIN_SUCCESS userId={}", user.getId());
        return new LoginResponse(jwtUtil.generateToken(user.getEmail(), user.getRole()), UserMapper.toResponse(user));
    }
}
