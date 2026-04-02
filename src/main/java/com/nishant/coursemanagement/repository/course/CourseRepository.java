package com.nishant.coursemanagement.repository.course;

import com.nishant.coursemanagement.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByIdAndIsActiveTrue(Long id);
    @Query("""
SELECT c FROM Course c
WHERE (:title IS NULL OR LOWER(c.title) LIKE :title)
AND (:active IS NULL OR c.isActive = :active)
AND (:instructorId IS NULL OR c.instructor.id = :instructorId)
""")
    Page<Course> findCourses(
            @Param("title") String title,
            @Param("active") Boolean active,
            @Param("instructorId") Long instructorId,
            Pageable pageable
    );
}
