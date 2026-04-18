package com.nishant.coursemanagement.integration.controller.course;

import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseUpdateRequest;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.integration.BaseIntegrationTest;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import static com.nishant.coursemanagement.entity.Role.INSTRUCTOR;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class CourseFlowIT extends BaseIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    private static final String ROOT_ENDPOINT = "/courses";
    private static final String ACTIVE_ENDPOINT = ROOT_ENDPOINT + "/active";
    private static final String MY_ENDPOINT = ROOT_ENDPOINT + "/my";
    private static final String ID_ENDPOINT = ROOT_ENDPOINT + "/%d";
    private static final String ACTIVE_ID_ENDPOINT = ACTIVE_ENDPOINT + "/%d";

    private static final String NEW_TITLE = "New Course Title";
    private static final String NEW_DESCRIPTION = "New Course Description";
    private static final String DESCRIPTION_TEXT = "Description";
    private static final String PAGE_PARAM = "page";
    private static final String SIZE_PARAM = "size";
    private static final String PAGE_0 = "0";
    private static final String SIZE_5 = "5";
    private static final String TITLE_PARAM = "title";
    private static final String INSTRUCTOR_ID_PARAM = "instructorId";
    private static final String ACTIVE_PARAM = "isActive";
    private static final String TRUE_VALUE = "true";

    private CourseRequest courseRequest;
    private CourseUpdateRequest courseUpdateRequest;
    private CoursePatchRequest coursePatchRequest;

    private void buildCourseRequest(){
        courseRequest = CourseRequest.builder()
                .title(TITLE)
                .description(DESCRIPTION)
                .build();
    }

    private void buildCourseUpdateRequest(){
        courseUpdateRequest = CourseUpdateRequest.builder()
                .title(NEW_TITLE)
                .description(NEW_DESCRIPTION)
                .build();
    }

    private void buildCoursePatchRequest(){
        coursePatchRequest = CoursePatchRequest.builder()
                .title(NEW_TITLE)
                .build();
    }

    @Nested
    class CreateTests{

        @Test
        void shouldCreateCourse_whenInstructor() throws Exception {
            setInstructorToken();
            buildCourseRequest();
            mockMvc.perform(post(ROOT_ENDPOINT)
                    .with(auth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(courseRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value(courseRequest.title()))
                    .andExpect(jsonPath("$.description").value(courseRequest.description()))
                    .andExpect(jsonPath("$.maxSeats").value(20));
        }

        @Test
        void shouldReturnForbidden_whenCreateCourseAsNonInstructor() throws Exception {
            setStudentToken();
            buildCourseRequest();
            mockMvc.perform(post(ROOT_ENDPOINT)
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(courseRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class UpdateTests{

        @Test
        void shouldUpdateCourseSuccessfully_whenOwner() throws Exception {
            setInstructorToken();
            buildCourseUpdateRequest();
            mockMvc.perform(put(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(courseUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(courseUpdateRequest.title()))
                    .andExpect(jsonPath("$.description").value(courseUpdateRequest.description()))
                    .andExpect(jsonPath("$.maxSeats").value(testCourse.getMaxSeats()));
        }

        @Test
        void shouldReturnNotFound_whenUpdateCourseAsNonOwner() throws Exception {
            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            token = generateToken(dummyUser);
            buildCourseUpdateRequest();
            mockMvc.perform(put(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(courseUpdateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class PatchTests{
        @Test
        void shouldPatchCourseSuccessfully_whenOwner() throws Exception {
            setInstructorToken();
            buildCoursePatchRequest();
            mockMvc.perform(patch(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(coursePatchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(coursePatchRequest.title()))
                    .andExpect(jsonPath("$.description").value(testCourse.getDescription()))
                    .andExpect(jsonPath("$.maxSeats").value(testCourse.getMaxSeats()));
        }

        @Test
        void shouldReturnNotFound_whenPatchCourseAsNonOwner() throws Exception {
            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            token = generateToken(dummyUser);
            buildCoursePatchRequest();
            mockMvc.perform(patch(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(coursePatchRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnBadRequest_whenAllFieldsAreEmpty() throws Exception {
            setInstructorToken();
            coursePatchRequest = new CoursePatchRequest("    ", null, null);
            mockMvc.perform(patch(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(coursePatchRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RetrievalTests {

        @Test
        void shouldGetCoursesWithPagination() throws Exception {
            setAdminToken();
            for (int i = 0; i < COURSES_TO_BE_CREATED; i++) {
                buildCourse("Course " + i, DESCRIPTION_TEXT + " " + i, MAX_SEATS, testUser);
            }

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .with(auth())
                            .param(PAGE_PARAM, PAGE_0)
                            .param(SIZE_PARAM, SIZE_5))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.pageNumber").value(0))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(courseRepository.count()));
        }

        @Test
        void shouldFilterCoursesByTitleInstructorAndActive() throws Exception {
            setAdminToken();
            String matchingTitle = "Filtered Course";

            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            buildCourse(matchingTitle, "Matching Course", MAX_SEATS, testUser);
            buildCourse("Other Course", "Different title", MAX_SEATS, testUser);
            buildCourse(matchingTitle, "Different instructor", MAX_SEATS, dummyUser);
            buildCourse(matchingTitle, "Inactive same instructor", MAX_SEATS, testUser);
            dummyCourse.setIsActive(false);
            courseRepository.saveAndFlush(dummyCourse);

            mockMvc.perform(get(ROOT_ENDPOINT)
                            .param(TITLE_PARAM, matchingTitle)
                            .param(INSTRUCTOR_ID_PARAM, String.valueOf(testUser.getId()))
                            .param(ACTIVE_PARAM, TRUE_VALUE)
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value(matchingTitle))
                    .andExpect(jsonPath("$.content[0].instructorId").value(testUser.getId()));
        }

        @Test
        void shouldGetCourseById_whenAdmin() throws Exception {
            setAdminToken();
            mockMvc.perform(get(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCourse.getId()))
                    .andExpect(jsonPath("$.title").value(testCourse.getTitle()))
                    .andExpect(jsonPath("$.description").value(testCourse.getDescription()))
                    .andExpect(jsonPath("$.instructorId").value(testUser.getId()))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        void shouldGetActiveCourseById_whenNonAdmin() throws Exception {
            mockMvc.perform(get(String.format(ACTIVE_ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCourse.getId()))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        void shouldGetAvailableCourses_whenNonAdmin() throws Exception {
            buildCourse("Public Active 1", DESCRIPTION_TEXT, MAX_SEATS, testUser);
            buildCourse("Public Active 2", DESCRIPTION_TEXT, MAX_SEATS, testUser);
            buildCourse("Public Inactive", DESCRIPTION_TEXT, MAX_SEATS, testUser);
            dummyCourse.setIsActive(false);
            courseRepository.saveAndFlush(dummyCourse);

            mockMvc.perform(get(ACTIVE_ENDPOINT)
                            .with(auth())
                            .param(PAGE_PARAM, PAGE_0)
                            .param(SIZE_PARAM, SIZE_5))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(5)))
                    .andExpect(jsonPath("$.content[*].isActive").value(everyItem(equalTo(true))));
        }

        @Test
        void shouldGetOwnCourses_whenInstructor() throws Exception {
            setInstructorToken();
            buildCourse("My Course 1", DESCRIPTION_TEXT, MAX_SEATS, testUser);
            buildCourse("My Course 2", DESCRIPTION_TEXT, MAX_SEATS, testUser);
            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            buildCourse("Other Instructor Course", DESCRIPTION_TEXT, MAX_SEATS, dummyUser);

            long expectedCount = courseRepository.findCourses(
                    null,
                    null,
                    testUser.getId(),
                    PageRequest.of(0, 5)
            ).getTotalElements();

            mockMvc.perform(get(MY_ENDPOINT)
                            .with(auth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value((int) expectedCount))
                    .andExpect(jsonPath("$.content[*].instructorId").value(everyItem(equalTo(testUser.getId().intValue()))))
                    .andExpect(jsonPath("$.totalElements").value(expectedCount));
        }
    }

    @Nested
    class DeactivationTests{
        @Test
        void shouldDeactivateCourseSuccessfully_whenOwner() throws Exception {
            setInstructorToken();
            mockMvc.perform(delete(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isNoContent());

            Course deactivated = courseRepository.findById(testCourse.getId()).orElseThrow();
            assert !deactivated.getIsActive();
        }

        @Test
        void shouldReturnNotFound_whenDeactivateCourseAsNonOwner() throws Exception {
            buildUser(DUMMY_NAME, DUMMY_EMAIL, INSTRUCTOR);
            token = generateToken(dummyUser);
            mockMvc.perform(delete(String.format(ID_ENDPOINT, testCourse.getId()))
                            .with(auth()))
                    .andExpect(status().isNotFound());
        }


    }

}
