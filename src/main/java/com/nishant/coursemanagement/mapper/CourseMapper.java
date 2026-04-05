package com.nishant.coursemanagement.mapper;

import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CourseMapper {

    public static Course toEntity(CourseRequest request, User instructor) {
        try {
            LogUtil.put("action", "MAP_COURSE_REQUEST_TO_ENTITY");
            LogUtil.put("title", request.title());
            LogUtil.put("instructorId", instructor.getId());
            log.info("Mapping course request to entity");
        } finally {
            LogUtil.clear();
        }

        if (request.maxSeats() == null) {
            request = new CourseRequest(request.title(), request.description(), 20L);
        }
        return Course.builder()
                .title(request.title())
                .description(request.description())
                .maxSeats(request.maxSeats())
                .enrolledStudents(0L)
                .instructor(instructor)
                .build();
    }

    public static CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .maxSeats(course.getMaxSeats())
                .availableSeats(course.getAvailableSeats())
                .instructorId(course.getInstructor().getId())
                .isActive(course.getIsActive())
                .build();
    }

}
