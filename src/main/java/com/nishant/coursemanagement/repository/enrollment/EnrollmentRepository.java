package com.nishant.coursemanagement.repository.enrollment;

import com.nishant.coursemanagement.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("""
    SELECT e FROM Enrollment e
    WHERE (:studentId IS NULL OR e.student.id = :studentId)
    AND (:courseId IS NULL OR e.course.id = :courseId)
    AND (:active IS NULL OR e.isActive = :active)
    """)
    Page<Enrollment> findEnrollments(
            @Param("studentId") Long studentId,
            @Param("courseId") Long courseId,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
