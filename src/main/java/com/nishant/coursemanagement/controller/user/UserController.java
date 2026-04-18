package com.nishant.coursemanagement.controller.user;


import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.user.*;
import com.nishant.coursemanagement.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserRequest user) {
        return userService.createUser(user);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return userService.refresh(request.refreshToken());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return userService.getAllUsers(name, email, isActive, pageable);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateUser(@Valid @RequestBody UserAdminUpdateRequest request, @PathVariable Long id) {
        return userService.updateUser(request, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse patchUser(@RequestBody UserAdminPatchRequest request, @PathVariable Long id) {
        return userService.patchUser(request, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deactivateUser(id);
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getMe() {
        return userService.getMe();
    }

    @PutMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateMe(@Valid @RequestBody UserSelfUpdateRequest request) {
        return userService.updateMe(request);
    }

    @PatchMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse patchMe(@RequestBody UserSelfPatchRequest request) {
        return userService.patchMe(request);
    }

    @DeleteMapping("/my")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe() {
        userService.deactivateMe();
    }

    @PostMapping("/my/change-password")
    @ResponseStatus(HttpStatus.OK)
    public PasswordChangeResponse passwordChange(@Valid @RequestBody NewPasswordRequest request) {
        return userService.changePassword(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorizationHeader,
                       @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        userService.logout(authorizationHeader.substring(BEARER_PREFIX.length()), refreshToken);
    }


}
