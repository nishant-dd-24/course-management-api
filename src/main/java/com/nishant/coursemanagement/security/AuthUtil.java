package com.nishant.coursemanagement.security;

import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AuthUtil {
    public final UserRepository userRepository;

    public User getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                        .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
