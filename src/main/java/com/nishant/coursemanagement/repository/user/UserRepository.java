package com.nishant.coursemanagement.repository.user;



import com.nishant.coursemanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
    SELECT u FROM User u
    WHERE (:name IS NULL OR LOWER(u.name) LIKE :name)
    AND (:email IS NULL OR LOWER(u.email) LIKE :email)
    AND (:active IS NULL OR u.isActive = :active)
    """)
    Page<User> findUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("active") Boolean active,
            Pageable pageable);
}
