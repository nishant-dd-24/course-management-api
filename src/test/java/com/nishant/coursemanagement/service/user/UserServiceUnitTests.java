package com.nishant.coursemanagement.service.user;

import com.nishant.coursemanagement.dto.user.LoginRequest;
import com.nishant.coursemanagement.dto.user.LoginResponse;
import com.nishant.coursemanagement.dto.user.NewPasswordRequest;
import com.nishant.coursemanagement.dto.user.PasswordChangeResponse;
import com.nishant.coursemanagement.dto.user.UserPatchRequest;
import com.nishant.coursemanagement.dto.user.UserRequest;
import com.nishant.coursemanagement.dto.user.UserResponse;
import com.nishant.coursemanagement.dto.user.UserUpdateRequest;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.user.UserUpdatedEvent;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.exception.custom.UnauthorizedException;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.security.JwtProperties;
import com.nishant.coursemanagement.security.JwtUtil;
import com.nishant.coursemanagement.service.BaseServiceTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static com.nishant.coursemanagement.entity.Role.ADMIN;
import static com.nishant.coursemanagement.entity.Role.STUDENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTests extends BaseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserQueryService userQueryService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String NAME = "John";
    private static final String EMAIL = "john@email.com";
    private static final String RAW_NAME = "   John  ";
    private static final String RAW_EMAIL = "   john@email.com  ";
    private static final String PASSWORD = "password";
    private static final String LOGIN_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded_password";
    private static final String UPDATED_NAME = "Jane";
    private static final String UPDATED_EMAIL = "jane@email.com";
    private static final String RAW_UPDATED_NAME = "   Jane   ";
    private static final String RAW_UPDATED_EMAIL = "   jane@email.com   ";
    private static final String OLD_PASSWORD = "old_password";
    private static final String INVALID_OLD_PASSWORD = "wrong_password";
    private static final String NEW_PASSWORD = "new_password";
    private static final String MISMATCH_CONFIRM_PASSWORD = "different_password";
    private static final String ENCODED_NEW_PASSWORD = "encoded_new_password";
    private static final String NOT_FOUND = "User not found";
    private static final String DUPLICATE_USER = "User already exists";
    private static final String DB_FAILURE = "DB Failure";
    private static final String TITLE_BLANK = "Name cannot be blank";
    private static final String DESCRIPTION_BLANK = "Email cannot be blank";
    private static final String PATCH_EMPTY = "At least one field must be provided for patching";
    private static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match";
    private static final String INVALID_CURRENT_PASSWORD = "Invalid current password";
    private static final String PASSWORD_CHANGED = "Password successfully changed";
    private static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    private static final String TOKEN = "jwt_token";
    private static final long EXPIRATION_SECONDS = 3600L;
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private UserRequest buildRequest(Role role){
        return new UserRequest(RAW_NAME, RAW_EMAIL, PASSWORD, role);
    }

    private UserRequest buildRequest(){
        return buildRequest(STUDENT);
    }

    private NewPasswordRequest buildChangePasswordRequest(String oldPassword, String newPassword, String confirmPassword) {
        return NewPasswordRequest.builder()
                .oldPassword(oldPassword)
                .newPassword(newPassword)
                .confirmPassword(confirmPassword)
                .build();
    }

    private UserUpdateRequest buildUpdateRequest(Role role) {
        return UserUpdateRequest.builder()
                .name(RAW_UPDATED_NAME)
                .email(RAW_UPDATED_EMAIL)
                .role(role)
                .build();
    }

    private UserPatchRequest buildPatchRequest(String name, String email, Role role) {
        return UserPatchRequest.builder()
                .name(name)
                .email(email)
                .role(role)
                .build();
    }

    private LoginRequest buildLoginRequest() {
        return LoginRequest.builder()
                .email(RAW_EMAIL)
                .password(LOGIN_PASSWORD)
                .build();
    }

    private User buildUser(boolean isActive, Role role){
        return User.builder()
                .id(1L)
                .name(NAME)
                .email(EMAIL)
                .password(PASSWORD)
                .role(role)
                .isActive(isActive)
                .build();
    }

    private User buildUser(boolean isActive){
        return buildUser(isActive, STUDENT);
    }

    private User captureSaved() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private void mockSaveWithId(Long id) {
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(id);
            return u;
        });
    }

    private void verifyNoQueryInteractions() {
        verifyNoInteractions(userQueryService);
    }


    private void verifyNoSecurityInteractions(){
        verifyNoInteractions(passwordEncoder, jwtUtil, jwtProperties);
    }
    @Nested
    class CreateUserTests{

        @Test
        void shouldCreateUserSuccessfully() {
            UserRequest request = buildRequest();
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(ENCODED_PASSWORD);
            mockSaveWithId(1L);
            UserResponse response = userService.createUser(request);
            assertNotNull(response);
            assertEquals(EMAIL, response.email());
            User saved = captureSaved();
            assertEquals(ENCODED_PASSWORD, saved.getPassword());
            assertEquals(EMAIL, saved.getEmail());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(1L, event.userId());
        }

        @Test
        void shouldReactivateUser_whenUserExistsButInactive() {
            UserRequest request = buildRequest();

            User existing = buildUser(false);

            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));
            mockSaveWithId(1L);

            UserResponse response = userService.createUser(request);
            assertEquals(EMAIL, response.email());

            User saved = captureSaved();
            assertEquals(EMAIL, saved.getEmail());
            assertTrue(saved.getIsActive());

            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(1L, event.userId());
        }

        @Test
        void shouldThrowException_whenUserAlreadyExistsAndActive() {
            UserRequest request = buildRequest();

            User existing = buildUser(true);

            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

            when(exceptionUtil.duplicate(DUPLICATE_USER))
                    .thenReturn(new DuplicateResourceException(DUPLICATE_USER));

            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> userService.createUser(request)
            );

            assertEquals(DUPLICATE_USER, ex.getMessage());

            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            UserRequest request = buildRequest();

            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.createUser(request)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class ChangePasswordTests {

        @Test
        void shouldThrowBadRequest_whenPasswordsDoNotMatch() {
            NewPasswordRequest request = buildChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, MISMATCH_CONFIRM_PASSWORD);
            mockBadRequestException(PASSWORDS_DO_NOT_MATCH);

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> userService.changePassword(request)
            );

            assertEquals(PASSWORDS_DO_NOT_MATCH, ex.getMessage());
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verify(authUtil, never()).getCurrentUser();
            verifyNoQueryInteractions();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowAccessDenied_whenOldPasswordIncorrect() {
            User currentUser = buildUser(1L);
            currentUser.setPassword(ENCODED_PASSWORD);
            NewPasswordRequest request = buildChangePasswordRequest(INVALID_OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(passwordEncoder.matches(INVALID_OLD_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
            when(exceptionUtil.accessDenied(INVALID_CURRENT_PASSWORD))
                    .thenReturn(new AccessDeniedException(INVALID_CURRENT_PASSWORD));

            AccessDeniedException ex = assertThrows(
                    AccessDeniedException.class,
                    () -> userService.changePassword(request)
            );

            assertEquals(INVALID_CURRENT_PASSWORD, ex.getMessage());
            verify(authUtil).getCurrentUser();
            verify(passwordEncoder).matches(INVALID_OLD_PASSWORD, ENCODED_PASSWORD);
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoQueryInteractions();
            verifyNoInteractions(jwtUtil, jwtProperties);
        }

        @Test
        void shouldChangePasswordSuccessfully() {
            User currentUser = buildUser(1L);
            currentUser.setPassword(ENCODED_PASSWORD);
            NewPasswordRequest request = buildChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(passwordEncoder.matches(OLD_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);
            mockSaveWithId(1L);

            PasswordChangeResponse response = userService.changePassword(request);

            assertNotNull(response);
            assertEquals(PASSWORD_CHANGED, response.confirmMessage());
            assertEquals(ENCODED_NEW_PASSWORD, currentUser.getPassword());
            verify(authUtil).getCurrentUser();
            verify(passwordEncoder).matches(OLD_PASSWORD, ENCODED_PASSWORD);
            verify(passwordEncoder).encode(NEW_PASSWORD);
            User saved = captureSaved();
            assertEquals(1L, saved.getId());
            assertEquals(ENCODED_NEW_PASSWORD, saved.getPassword());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(1L, event.userId());
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            User currentUser = buildUser(1L);
            currentUser.setPassword(ENCODED_PASSWORD);
            NewPasswordRequest request = buildChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(passwordEncoder.matches(OLD_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);
            when(userRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.changePassword(request)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class UpdateUserTests {

        @Test
        void shouldUpdateUserSuccessfully() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(2L);
            currentUser.setRole(STUDENT);
            UserUpdateRequest request = buildUpdateRequest(STUDENT);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            mockSaveWithId(userId);

            UserResponse response = userService.updateUser(request, userId);

            assertNotNull(response);
            assertEquals(UPDATED_NAME, response.name());
            assertEquals(UPDATED_EMAIL, response.email());
            assertEquals(STUDENT.name(), response.role());
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            verify(authUtil).getCurrentUser();
            User saved = captureSaved();
            assertEquals(UPDATED_NAME, saved.getName());
            assertEquals(UPDATED_EMAIL, saved.getEmail());
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowDuplicateException_whenEmailAlreadyExists() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            UserUpdateRequest request = buildUpdateRequest(STUDENT);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(true);
            when(exceptionUtil.duplicate(EMAIL_ALREADY_EXISTS))
                    .thenReturn(new DuplicateResourceException(EMAIL_ALREADY_EXISTS));

            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> userService.updateUser(request, userId)
            );

            assertEquals(EMAIL_ALREADY_EXISTS, ex.getMessage());
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            verify(authUtil, never()).getCurrentUser();
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldUpdateRole_whenCurrentUserIsAdmin() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(2L);
            currentUser.setRole(ADMIN);
            UserUpdateRequest request = buildUpdateRequest(ADMIN);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            mockSaveWithId(userId);

            UserResponse response = userService.updateUser(request, userId);

            assertNotNull(response);
            assertEquals(ADMIN.name(), response.role());
            verify(authUtil).getCurrentUser();
            User saved = captureSaved();
            assertEquals(ADMIN, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldNotUpdateRole_whenCurrentUserIsNotAdmin() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(2L);
            currentUser.setRole(STUDENT);
            UserUpdateRequest request = buildUpdateRequest(ADMIN);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            mockSaveWithId(userId);

            UserResponse response = userService.updateUser(request, userId);

            assertNotNull(response);
            assertEquals(STUDENT.name(), response.role());
            verify(authUtil).getCurrentUser();
            User saved = captureSaved();
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldUpdateCurrentUser() {
            Long userId = 1L;
            User currentUser = buildUser(true, STUDENT);
            UserUpdateRequest request = buildUpdateRequest(STUDENT);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(currentUser);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            mockSaveWithId(userId);

            UserResponse response = userService.updateMe(request);

            assertNotNull(response);
            assertEquals(UPDATED_NAME, response.name());
            assertEquals(UPDATED_EMAIL, response.email());
            assertEquals(STUDENT.name(), response.role());
            verify(authUtil, times(2)).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            User saved = captureSaved();
            assertEquals(UPDATED_NAME, saved.getName());
            assertEquals(UPDATED_EMAIL, saved.getEmail());
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(2L);
            currentUser.setRole(STUDENT);
            UserUpdateRequest request = buildUpdateRequest(STUDENT);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.updateUser(request, userId)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class PatchUserTests {

        @Test
        void shouldPatchUserSuccessfully() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(RAW_UPDATED_NAME, RAW_UPDATED_EMAIL, null);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            mockSaveWithId(userId);

            UserResponse response = userService.patchUser(request, userId);

            assertNotNull(response);
            assertEquals(UPDATED_NAME, response.name());
            assertEquals(UPDATED_EMAIL, response.email());
            assertEquals(STUDENT.name(), response.role());
            verify(authUtil).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            User saved = captureSaved();
            assertEquals(UPDATED_NAME, saved.getName());
            assertEquals(UPDATED_EMAIL, saved.getEmail());
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowBadRequest_whenPatchRequestEmpty() {
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(null, null, null);
            mockBadRequestException(PATCH_EMPTY);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> userService.patchUser(request, 1L)
            );

            assertEquals(PATCH_EMPTY, ex.getMessage());
            verify(authUtil).getCurrentUser();
            verify(userQueryService, never()).getUser(any(Long.class));
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowBadRequest_whenNameBlank() {
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest("   ", null, null);
            mockBadRequestException(TITLE_BLANK);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> userService.patchUser(request, 1L)
            );

            assertEquals(TITLE_BLANK, ex.getMessage());
            verify(authUtil).getCurrentUser();
            verify(userQueryService, never()).getUser(any(Long.class));
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowBadRequest_whenEmailBlank() {
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(null, "   ", null);
            mockBadRequestException(DESCRIPTION_BLANK);
            when(authUtil.getCurrentUser()).thenReturn(currentUser);

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> userService.patchUser(request, 1L)
            );

            assertEquals(DESCRIPTION_BLANK, ex.getMessage());
            verify(authUtil).getCurrentUser();
            verify(userQueryService, never()).getUser(any(Long.class));
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowDuplicateException_whenEmailAlreadyExists() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(null, RAW_UPDATED_EMAIL, null);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(true);
            when(exceptionUtil.duplicate(EMAIL_ALREADY_EXISTS))
                    .thenReturn(new DuplicateResourceException(EMAIL_ALREADY_EXISTS));

            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> userService.patchUser(request, userId)
            );

            assertEquals(EMAIL_ALREADY_EXISTS, ex.getMessage());
            verify(authUtil).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldUpdateRole_whenRoleProvidedAndDifferent() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(true, ADMIN);
            UserPatchRequest request = buildPatchRequest(null, null, ADMIN);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            mockSaveWithId(userId);

            UserResponse response = userService.patchUser(request, userId);

            assertNotNull(response);
            assertEquals(ADMIN.name(), response.role());
            verify(authUtil).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository, never()).existsByEmail(any());
            User saved = captureSaved();
            assertEquals(ADMIN, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldNotUpdateRole_whenCurrentUserNotAdmin() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(null, null, ADMIN);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            mockSaveWithId(userId);

            UserResponse response = userService.patchUser(request, userId);

            assertNotNull(response);
            assertEquals(STUDENT.name(), response.role());
            verify(authUtil).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository, never()).existsByEmail(any());
            User saved = captureSaved();
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldPatchCurrentUserSuccessfully() {
            Long userId = 1L;
            User currentUser = buildUser(true, STUDENT);
            User user = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(RAW_UPDATED_NAME, RAW_UPDATED_EMAIL, null);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.existsByEmail(UPDATED_EMAIL)).thenReturn(false);
            mockSaveWithId(userId);

            UserResponse response = userService.patchMe(request);

            assertNotNull(response);
            assertEquals(UPDATED_NAME, response.name());
            assertEquals(UPDATED_EMAIL, response.email());
            assertEquals(STUDENT.name(), response.role());
            verify(authUtil, times(2)).getCurrentUser();
            verify(userQueryService).getUser(userId);
            verify(userRepository).existsByEmail(UPDATED_EMAIL);
            User saved = captureSaved();
            assertEquals(UPDATED_NAME, saved.getName());
            assertEquals(UPDATED_EMAIL, saved.getEmail());
            assertEquals(STUDENT, saved.getRole());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            Long userId = 1L;
            User user = buildUser(true, STUDENT);
            User currentUser = buildUser(true, STUDENT);
            UserPatchRequest request = buildPatchRequest(RAW_UPDATED_NAME, null, null);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.patchUser(request, userId)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class DeleteUserTests {

        @Test
        void shouldDeactivateUserSuccessfully() {
            Long userId = 1L;
            User user = buildUser(true);

            when(userQueryService.getUser(userId)).thenReturn(user);
            mockSaveWithId(userId);

            userService.deactivateUser(userId);

            assertFalse(user.getIsActive());
            verify(userQueryService).getUser(userId);
            User saved = captureSaved();
            assertEquals(userId, saved.getId());
            assertFalse(saved.getIsActive());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldThrowException_whenUserNotFound() {
            Long userId = 1L;
            when(userQueryService.getUser(userId)).thenThrow(new ResourceNotFoundException(NOT_FOUND));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.deactivateUser(userId)
            );

            assertEquals(NOT_FOUND, ex.getMessage());
            verify(userQueryService).getUser(userId);
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoSecurityInteractions();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldDeactivateCurrentUser() {
            Long userId = 1L;
            User currentUser = buildUser(userId);
            User user = buildUser(true);

            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(userQueryService.getUser(userId)).thenReturn(user);
            mockSaveWithId(userId);

            userService.deactivateMe();

            assertFalse(user.getIsActive());
            verify(authUtil).getCurrentUser();
            verify(userQueryService).getUser(userId);
            User saved = captureSaved();
            assertEquals(userId, saved.getId());
            assertFalse(saved.getIsActive());
            UserUpdatedEvent event = captureEvent(UserUpdatedEvent.class);
            assertEquals(userId, event.userId());
            verifyNoSecurityInteractions();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            Long userId = 1L;
            User user = buildUser(true);

            when(userQueryService.getUser(userId)).thenReturn(user);
            when(userRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.deactivateUser(userId)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class LoginTests {

        @Test
        void shouldThrowUnauthorized_whenUserInactive() {
            User user = buildUser(false, STUDENT);
            LoginRequest request = buildLoginRequest();

            when(userQueryService.getUser(EMAIL)).thenReturn(user);
            when(exceptionUtil.unauthorized(INVALID_CREDENTIALS))
                    .thenReturn(new UnauthorizedException(INVALID_CREDENTIALS));

            UnauthorizedException ex = assertThrows(
                    UnauthorizedException.class,
                    () -> userService.login(request)
            );

            assertEquals(INVALID_CREDENTIALS, ex.getMessage());
            verify(userQueryService).getUser(EMAIL);
            verifyNoSecurityInteractions();
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldThrowUnauthorized_whenPasswordIncorrect() {
            User user = buildUser(true, STUDENT);
            user.setPassword(ENCODED_PASSWORD);
            LoginRequest request = buildLoginRequest();

            when(userQueryService.getUser(EMAIL)).thenReturn(user);
            when(passwordEncoder.matches(LOGIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
            when(exceptionUtil.unauthorized(INVALID_CREDENTIALS))
                    .thenReturn(new UnauthorizedException(INVALID_CREDENTIALS));

            UnauthorizedException ex = assertThrows(
                    UnauthorizedException.class,
                    () -> userService.login(request)
            );

            assertEquals(INVALID_CREDENTIALS, ex.getMessage());
            verify(userQueryService).getUser(EMAIL);
            verify(passwordEncoder).matches(LOGIN_PASSWORD, ENCODED_PASSWORD);
            verifyNoInteractions(jwtProperties, jwtUtil);
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldLoginSuccessfully() {
            User user = buildUser(true, STUDENT);
            user.setPassword(ENCODED_PASSWORD);
            LoginRequest request = buildLoginRequest();

            when(userQueryService.getUser(EMAIL)).thenReturn(user);
            when(passwordEncoder.matches(LOGIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(EMAIL, STUDENT)).thenReturn(TOKEN);
            when(jwtProperties.getExpirationSeconds()).thenReturn(EXPIRATION_SECONDS);

            LoginResponse response = userService.login(request);

            assertNotNull(response);
            assertEquals(TOKEN, response.token());
            assertEquals(EXPIRATION_SECONDS, response.expiresIn());
            assertNotNull(response.expiresAt());
            assertNotNull(response.user());
            assertEquals(EMAIL, response.user().email());
            assertEquals(STUDENT.name(), response.user().role());
            verify(userQueryService).getUser(EMAIL);
            verify(passwordEncoder).matches(LOGIN_PASSWORD, ENCODED_PASSWORD);
            verify(jwtUtil).generateToken(EMAIL, STUDENT);
            verify(jwtProperties).getExpirationSeconds();
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldThrowUnauthorized_whenUserNotFound() {
            LoginRequest request = buildLoginRequest();

            when(userQueryService.getUser(EMAIL))
                    .thenThrow(new ResourceNotFoundException(NOT_FOUND));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.login(request)
            );

            assertEquals(NOT_FOUND, ex.getMessage());

            verify(userQueryService).getUser(EMAIL);
            verifyNoSecurityInteractions();
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
        }

        @Test
        void shouldSetExpirationCorrectly() {
            User user = buildUser(true, STUDENT);
            user.setPassword(ENCODED_PASSWORD);
            LoginRequest request = buildLoginRequest();
            Instant beforeLogin = Instant.now();

            when(userQueryService.getUser(EMAIL)).thenReturn(user);
            when(passwordEncoder.matches(LOGIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(EMAIL, STUDENT)).thenReturn(TOKEN);
            when(jwtProperties.getExpirationSeconds()).thenReturn(EXPIRATION_SECONDS);

            LoginResponse response = userService.login(request);

            Instant afterLogin = Instant.now();
            Instant earliestExpected = beforeLogin.plusSeconds(EXPIRATION_SECONDS);
            Instant latestExpected = afterLogin.plusSeconds(EXPIRATION_SECONDS);

            assertEquals(EXPIRATION_SECONDS, response.expiresIn());
            assertFalse(response.expiresAt().isBefore(earliestExpected));
            assertFalse(response.expiresAt().isAfter(latestExpected));
            verify(jwtProperties).getExpirationSeconds();
            verify(userQueryService).getUser(EMAIL);
            verify(passwordEncoder).matches(LOGIN_PASSWORD, ENCODED_PASSWORD);
            verify(jwtUtil).generateToken(EMAIL, STUDENT);
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
            verifyNoInteractions(authUtil);
        }

        @Test
        void shouldThrowException_whenUserQueryFails() {
            LoginRequest request = buildLoginRequest();

            when(userQueryService.getUser(EMAIL)).thenThrow(new RuntimeException(DB_FAILURE));

            RuntimeException ex = assertThrows(
                    RuntimeException.class,
                    () -> userService.login(request)
            );

            assertEquals(DB_FAILURE, ex.getMessage());
            verify(userQueryService).getUser(EMAIL);
            verifyNoInteractions(passwordEncoder, jwtUtil, jwtProperties, authUtil);
            verify(userRepository, never()).save(any());
            verifyNoEventPublished();
        }
    }

}
