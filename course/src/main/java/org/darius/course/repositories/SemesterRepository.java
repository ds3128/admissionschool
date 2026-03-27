package org.darius.course.repositories;

import org.darius.course.entities.Semester;
import org.darius.course.enums.SemesterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    Optional<Semester> findByIsCurrent(boolean isCurrent);

    Optional<Semester> findByStatus(SemesterStatus status);

    List<Semester> findByAcademicYear(String academicYear);

    // Semestres dont la date de début est atteinte mais toujours UPCOMING
    @Query("""
        SELECT s FROM Semester s
        WHERE s.status = 'UPCOMING'
          AND s.startDate <= :today
        """)
    List<Semester> findUpcomingToActivate(@Param("today") LocalDate today);
}