package com.proj.ScholarAssist.repository;

import com.proj.ScholarAssist.entity.StudentProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** Database access for student profiles. */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    /** Finds a profile through its unique owning-user relationship. */
    Optional<StudentProfile> findByUser_Id(Long userId);

    /** Finds the profile owned by the authenticated account. */
    Optional<StudentProfile> findByUser_EmailIgnoreCase(String email);

    /** Locks a student row while a duplicate-sensitive application is created. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from StudentProfile student where student.id = :studentId")
    Optional<StudentProfile> findByIdForUpdate(@Param("studentId") Long studentId);
}
