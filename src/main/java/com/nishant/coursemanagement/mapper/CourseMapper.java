package com.nishant.coursemanagement.mapper;

import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;

public class CourseMapper {

    public static Course toEntity(CourseRequest request, User instructor){
        return Course.builder()
                .title(request.title())
                .description(request.description())
                .instructor(instructor)
                .build();
    }

    public static CourseResponse toResponse(Course course){
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .instructorId(course.getInstructor().getId())
                .build();
    }

}
