package com.nishant.coursemanagement.mapper;

import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CourseMapper {

    public static Course toEntity(CourseRequest request, User instructor){

        log.info("action=MAP_COURSE_REQUEST_TO_ENTITY title={} instructorId={}", request.title(), instructor.getId());
        log.info("CourseRequest details: {}", request);

        if(request.maxSeats() == null){
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

    public static CourseResponse toResponse(Course course){
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .maxSeats(course.getMaxSeats())
                .instructorId(course.getInstructor().getId())
                .build();
    }

}
