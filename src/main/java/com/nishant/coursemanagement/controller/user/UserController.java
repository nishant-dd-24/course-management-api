package com.nishant.coursemanagement.controller.user;


import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            tags = {"Auth"},
            summary = "Register a user",
            description = "Create a new user account",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    public UserResponse createUser(@Valid @RequestBody UserRequest user) {
        return userService.createUser(user);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            tags = {"Auth"},
            summary = "Login",
            description = "Authenticate user and return access/refresh tokens",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            tags = {"Auth"},
            summary = "Refresh access token",
            description = "Issue a new access token using a valid refresh token",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return userService.refresh(request.refreshToken());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get all users",
            description = "Retrieve paginated and filtered users (ADMIN only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public PageResponse<UserResponse> getAllUsers(@Valid @ParameterObject @ModelAttribute UserSearchRequest request) {
        return userService.getAllUsers(request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get user by ID", description = "Retrieve a user by ID (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update user", description = "Fully update a user (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponse updateUser(@Valid @RequestBody UserAdminUpdateRequest request, @PathVariable Long id) {
        return userService.updateUser(request, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Patch user", description = "Partially update a user (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public UserResponse patchUser(@RequestBody UserAdminPatchRequest request, @PathVariable Long id) {
        return userService.patchUser(request, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate user", description = "Deactivate a user account (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public void deleteUser(@PathVariable Long id) {
        userService.deactivateUser(id);
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get my profile", description = "Retrieve the authenticated user's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public UserResponse getMe() {
        return userService.getMe();
    }

    @PutMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update my profile", description = "Fully update the authenticated user's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public UserResponse updateMe(@Valid @RequestBody UserSelfUpdateRequest request) {
        return userService.updateMe(request);
    }

    @PatchMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Patch my profile", description = "Partially update the authenticated user's profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public UserResponse patchMe(@RequestBody UserSelfPatchRequest request) {
        return userService.patchMe(request);
    }

    @DeleteMapping("/my")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate my account", description = "Deactivate the authenticated user's account")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public void deleteMe() {
        userService.deactivateMe();
    }

    @PostMapping("/my/change-password")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Change password", description = "Change password for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public PasswordChangeResponse passwordChange(@Valid @RequestBody NewPasswordRequest request) {
        return userService.changePassword(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            tags = {"Auth"},
            summary = "Logout",
            description = "Invalidate current access token and optional refresh token",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid Authorization header"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public void logout(
            @Parameter(
                    description = "Bearer access token",
                    example = "Bearer <token>",
                    schema = @Schema(type = "string")
            )
            @RequestHeader("Authorization") String authorizationHeader,
            @Parameter(description = "Optional refresh token to invalidate", example = "refresh-token-value")
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        userService.logout(authorizationHeader.substring(BEARER_PREFIX.length()), refreshToken);
    }


}
