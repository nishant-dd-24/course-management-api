package com.nishant.coursemanagement.integration.controller;

import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.repository.user.UserRepository;
import com.nishant.coursemanagement.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.nishant.coursemanagement.entity.Role.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ActiveProfiles("test")

public abstract class BaseIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    protected JwtUtil jwtUtil;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected static final String NAME = "Test User";
    protected static final String EMAIL = "test@email.com";
    protected static final String DUMMY_NAME = "Dummy User";
    protected static final String DUMMY_EMAIL = "dummyuser@email.com";
    protected static final String PASSWORD = "password";
    protected static final String TITLE = "Course Title";
    protected static final String DESCRIPTION = "Course Description";

    protected static final Long USERS_TO_BE_CREATED = 6L;
    protected static final Long COURSES_TO_BE_CREATED = 10L;
    protected static final Long MAX_SEATS = 20L;

    protected User testUser;
    protected User dummyUser;
    protected Course testCourse;
    protected Course dummyCourse;
    protected String token;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        courseRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                User.builder()
                        .name(NAME)
                        .email(EMAIL)
                        .password(passwordEncoder.encode(PASSWORD))
                        .role(INSTRUCTOR)
                        .isActive(true)
                        .build()
        );

        testCourse = courseRepository.saveAndFlush(
                Course.builder()
                        .title(TITLE)
                        .description(DESCRIPTION)
                        .maxSeats(MAX_SEATS)
                        .instructor(testUser)
                        .enrolledStudents(0L)
                        .isActive(true)
                        .build()
        );

        token = generateToken(testUser);
    }

    protected String generateToken(User user) {
        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    protected String authHeader() {
        return "Bearer " + token;
    }

    protected RequestPostProcessor auth() {
        return request -> {
            request.addHeader("Authorization", authHeader());
            return request;
        };
    }

    protected String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    protected void setStudentToken(){
        testUser.setRole(STUDENT);
        userRepository.saveAndFlush(testUser);
        testUser = userRepository.findById(testUser.getId()).orElseThrow();
        token = generateToken(testUser);
    }

    protected void setAdminToken(){
        testUser.setRole(ADMIN);
        userRepository.saveAndFlush(testUser);
        testUser = userRepository.findById(testUser.getId()).orElseThrow();
        token = generateToken(testUser);
    }

    protected void setInstructorToken(){
        testUser.setRole(INSTRUCTOR);
        userRepository.saveAndFlush(testUser);
        testUser = userRepository.findById(testUser.getId()).orElseThrow();
        token = generateToken(testUser);
    }

    protected void buildUser(String name, String email){
        buildUser(name, email, STUDENT);
    }

    protected void buildUser(String name, String email, Role role){
        dummyUser = userRepository.saveAndFlush(
                User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(PASSWORD))
                        .role(role)
                        .isActive(true)
                        .build()
        );
    }

    protected void buildThisManyUsers(){
        for (int i = 0; i < USERS_TO_BE_CREATED; i++) {
            userRepository.save(
                    User.builder()
                            .name("User " + i)
                            .email("user" + i + "_" + System.nanoTime() + "@email.com")
                            .password(passwordEncoder.encode(PASSWORD))
                            .role(STUDENT)
                            .isActive(true)
                            .build()
            );
        }
        userRepository.flush();
    }

    protected void buildCourse(String title, String description, Long maxSeats, User user) {
        dummyCourse = courseRepository.saveAndFlush(
                Course.builder()
                        .title(title)
                        .description(description)
                        .maxSeats(maxSeats)
                        .instructor(user)
                        .enrolledStudents(0L)
                        .isActive(true)
                        .build()
        );
    }
}