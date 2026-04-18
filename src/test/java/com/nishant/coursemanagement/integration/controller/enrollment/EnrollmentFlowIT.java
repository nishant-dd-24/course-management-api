package com.nishant.coursemanagement.integration.controller.enrollment;

import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.nishant.coursemanagement.entity.Role.INSTRUCTOR;
import static com.nishant.coursemanagement.entity.Role.STUDENT;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class EnrollmentFlowIT extends BaseIntegrationTest {



    private static final String ROOT_ENDPOINT = "/enrollments";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/%d";
    private static final String MY_ENDPOINT = ROOT_ENDPOINT + "/my";
    private static final String SECOND_STUDENT_NAME = "Second Student";
    private static final String SECOND_STUDENT_EMAIL = "secondstudent@email.com";
    private static final String FULL_COURSE_TITLE = "Single Seat Course";
    private static final String FULL_COURSE_DESCRIPTION = "Only one seat";
    private static final int THREAD_COUNT = 5;
    private static final long CONCURRENT_MAX_SEATS = 2L;

    private RequestPostProcessor authWithToken(String jwt) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + jwt);
            return request;
        };
    }

    @Nested
    class CoreTests {
        @Test
        void shouldEnrollSuccessfully() throws Exception {
            setStudentToken();

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.studentId").value(testUser.getId()))
                    .andExpect(jsonPath("$.courseId").value(testCourse.getId()));

            Enrollment enrollment = findEnrollment(testUser.getId(), testCourse.getId());

            assertNotNull(enrollment);
            assertTrue(enrollment.getIsActive());
        }

        @Test
        void shouldThrowBadRequest_whenCourseIsFull() throws Exception {
            setStudentToken();

            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            buildCourse(FULL_COURSE_TITLE, FULL_COURSE_DESCRIPTION, 1L, dummyUser);
            Long fullCourseId = dummyCourse.getId();

            mockMvc.perform(post(String.format(ID_ENDPOINT, fullCourseId))
                            .with(auth()))
                    .andExpect(status().isOk());

            buildUser(SECOND_STUDENT_NAME, SECOND_STUDENT_EMAIL);
            token = generateToken(dummyUser);

            mockMvc.perform(post(String.format(ID_ENDPOINT, fullCourseId))
                            .with(auth()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldThrowConflict_whenDuplicateEnrollment() throws Exception {
            setStudentToken();

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk());

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldUnenrollSuccessfully() throws Exception {
            setStudentToken();

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk());

            mockMvc.perform(delete(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isNoContent());

            Enrollment enrollment = findEnrollment(testUser.getId(), testCourse.getId());

            assertNotNull(enrollment);
            assertFalse(enrollment.getIsActive());
        }

    }

    @Nested
    class RetrievalTests{

        @Test
        void shouldGetOwnEnrollments() throws Exception {
            setStudentToken();

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk());

            mockMvc.perform(get(MY_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[*].studentId").value(everyItem(equalTo(testUser.getId().intValue()))));
        }

        @Test
        void shouldGetCourseEnrollments_whenInstructor() throws Exception {
            setInstructorToken();
            buildUser(SECOND_STUDENT_NAME, SECOND_STUDENT_EMAIL, STUDENT);
            buildEnrollment(dummyUser, testCourse, true);

            mockMvc.perform(get(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[*].courseId").value(everyItem(equalTo(testCourse.getId().intValue()))));
        }

    }

    @Nested
    class SecurityTests{

        @Test
        void shouldReturnUnauthorized_whenEnrollWithoutToken() throws Exception {
            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnForbidden_whenEnrollWithInvalidRole() throws Exception {
            setInstructorToken();

            mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isForbidden());
        }

    }

    @Nested
    class ConcurrencyTests{

        @Test
        void shouldAllowOnlyLimitedEnrollments_whenConcurrentRequests() throws Exception {
            testCourse.setMaxSeats(CONCURRENT_MAX_SEATS);
            testCourse.setEnrolledStudents(0L);
            courseRepository.saveAndFlush(testCourse);

            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                buildUser("concurrent-student-" + i, "concurrent-student-" + i + "@email.com", STUDENT);
                tokens.add(generateToken(dummyUser));
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
            List<Future<Integer>> futures = new ArrayList<>();

            for (String jwt : tokens) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    return mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                                    .with(authWithToken(jwt)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            readyLatch.await();
            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            int successCount = 0;
            for (Future<Integer> future : futures) {
                int statusCode = future.get();
                if (statusCode == 200 || statusCode == 201) {
                    successCount++;
                }
            }

            long activeEnrollments = enrollmentRepository.findEnrollments(
                    null,
                    testCourse.getId(),
                    true,
                    PageRequest.of(0, 20)
            ).getTotalElements();

            assertEquals((int) CONCURRENT_MAX_SEATS, successCount);
            assertEquals(CONCURRENT_MAX_SEATS, activeEnrollments);
            assertEquals(CONCURRENT_MAX_SEATS,
                    courseRepository.findById(testCourse.getId()).orElseThrow().getEnrolledStudents());
        }

        @Test
        void shouldRejectExcessEnrollments_whenConcurrentRequests() throws Exception {
            testCourse.setMaxSeats(CONCURRENT_MAX_SEATS);
            testCourse.setEnrolledStudents(0L);
            courseRepository.saveAndFlush(testCourse);

            List<String> tokens = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                buildUser("overflow-student-" + i, "overflow-student-" + i + "@email.com", STUDENT);
                tokens.add(generateToken(dummyUser));
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
            List<Future<Integer>> futures = new ArrayList<>();

            for (String jwt : tokens) {
                futures.add(executor.submit(() -> {
                    readyLatch.countDown();
                    startLatch.await();
                    return mockMvc.perform(post(String.format(ID_ENDPOINT, testCourse.getId()))
                                    .with(authWithToken(jwt)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            }

            readyLatch.await();
            startLatch.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

            int successCount = 0;
            int failureCount = 0;

            for (Future<Integer> future : futures) {
                int statusCode = future.get();
                if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
                    successCount++;
                }
                if (HttpStatus.valueOf(statusCode).is4xxClientError()) {
                    failureCount++;
                }
            }

            assertEquals((int) CONCURRENT_MAX_SEATS, successCount);
            assertTrue(failureCount > 0);
        }

    }
}
