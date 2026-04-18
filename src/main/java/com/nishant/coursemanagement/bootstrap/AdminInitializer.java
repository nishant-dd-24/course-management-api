package com.nishant.coursemanagement.bootstrap;

import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import com.nishant.coursemanagement.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.nishant.coursemanagement.entity.Role.ADMIN;
import static com.nishant.coursemanagement.log.annotation.LogLevel.INFO;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;

@RequiredArgsConstructor
@Component
@Transactional
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Override
    @Loggable(action = "CREATE_BOOTSTRAP_ADMIN")
    public void run(@NonNull ApplicationArguments args) {

        if (adminEmail == null || adminPassword == null || adminName == null) {
            LogUtil.log(log, WARN, "BOOTSTRAP_ADMIN_ENV_VARS_NOT_CONFIGURED", "Admin bootstrap skipped: required environment variables not configured");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            LogUtil.log(log, INFO, "ADMIN_ALREADY_EXISTS", "Admin bootstrap skipped: user with configured admin email already exists");
            return;
        }

        User admin = User.builder()
                .name(adminName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(ADMIN)
                .isActive(true)
                .build();

        userRepository.save(admin);
    }
}
