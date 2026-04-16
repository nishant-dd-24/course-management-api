package com.nishant.coursemanagement.repository.course;

import com.nishant.coursemanagement.entity.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByIdAndIsActiveTrue(Long id);

    @Query("""
            SELECT c FROM Course c
            WHERE (:title IS NULL OR LOWER(c.title) LIKE :title)
            AND (:isActive IS NULL OR c.isActive = :isActive)
            AND (:instructorId IS NULL OR c.instructor.id = :instructorId)
            """)
    Page<Course> findCourses(
            @Param("title") String title,
            @Param("isActive") Boolean isActive,
            @Param("instructorId") Long instructorId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :courseId AND c.isActive = true")
    Optional<Course> findByIdForUpdate(@Param("courseId") Long courseId);
}
