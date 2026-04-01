package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.CourseMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.util.Sanitizer;
import com.nishant.coursemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseServiceImpl implements CourseService{

    private final CourseRepository courseRepository;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;

    private void validateCourseOwnership(Course course, User currentUser) {
        if(!course.getInstructor().getId().equals(currentUser.getId())) {
            throw exceptionUtil.notFound("Course not found");
        }
    }

    @Override
    public CourseResponse createCourse(CourseRequest request){
        User currentUser = authUtil.getCurrentUser();
        request = Sanitizer.sanitizeCourseRequest(request);
        log.info("action=CREATE_COURSE userId={}", currentUser.getId());
        Course course = CourseMapper.toEntity(request, currentUser);
        log.info("action=CREATE_COURSE_SUCCESS userId={} courseId={}", currentUser.getId(), course.getId());
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse getCourse(Long id){
        log.debug("action=GET_COURSE courseId={}", id);
        return CourseMapper.toResponse(courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found")));
    }

    @Override
    public CourseResponse getActiveCourse(Long id){
        log.debug("action=GET_ACTIVE_COURSE courseId={}", id);
        return CourseMapper.toResponse(courseRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found")));
    }

    @Override
    public Page<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable){
        title = StringUtil.normalize(title);
        log.debug("action=GET_ALL_COURSES title={} active={} instructorId={} pageNumber={} pageSize={}", title, active, instructorId, pageable.getPageNumber(), pageable.getPageSize());
        return courseRepository
                .findCourses(title, active, instructorId, pageable)
                .map(CourseMapper::toResponse);
    }

    @Override
    public Page<CourseResponse> getAllActiveCourses(String title, Long instructorId, Pageable pageable){
        return getAllCourses(title, true, instructorId, pageable);
    }

    @Override
    public Page<CourseResponse> getMyCourses(String title, Boolean active, Pageable pageable){
        return getAllCourses(title, active, authUtil.getCurrentUser().getId(), pageable);
    }

    @Override
    public CourseResponse updateCourse(Long id, CourseRequest request){
        User currentUser = authUtil.getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        validateCourseOwnership(course, currentUser);
        request = Sanitizer.sanitizeCourseRequest(request);
        log.info("action=UPDATE_COURSE courseId={} userId={}", id, currentUser.getId());
        course.setTitle(request.title());
        course.setDescription(request.description());
        log.info("action=UPDATE_COURSE_SUCCESS courseId={} userId={}", id, currentUser.getId());
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    private void applyPatch(Course course, CoursePatchRequest request) {
        Optional.ofNullable(request.title())
                .map(StringUtil::normalize)
                .ifPresent(course::setTitle);
        Optional.ofNullable(request.description())
                .map(StringUtil::normalize)
                .ifPresent(course::setDescription);
    }
    private void validatePatchRequest(CoursePatchRequest request) {
        if(request.title()==null && request.description()==null) {
            throw exceptionUtil.badRequest("At least one field must be provided for patching");
        }
        if (StringUtil.isBlankButNotNull(request.title())) {
            throw exceptionUtil.badRequest("Title cannot be blank");
        }
        if (StringUtil.isBlankButNotNull(request.description())) {
            throw exceptionUtil.badRequest("Description cannot be blank");
        }
    }

    @Override
    public CourseResponse patchCourse(Long id, CoursePatchRequest request){
        validatePatchRequest(request);
        User currentUser = authUtil.getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        validateCourseOwnership(course, currentUser);
        log.info("action=PATCH_COURSE courseId={} userId={}", id, currentUser.getId());
        log.debug("action=PATCH_COURSE_PAYLOAD courseId={} userId={} request={}", id, currentUser.getId(), request);
        applyPatch(course, request);
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public void deactivateCourse(Long id){
        User currentUser = authUtil.getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        validateCourseOwnership(course, currentUser);
        log.warn("action=DEACTIVATE_COURSE courseId={} userId={}", id, currentUser.getId());
        course.setIsActive(false);
        courseRepository.save(course);
    }
}
