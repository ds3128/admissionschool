package org.darius.course.repositories;

import org.darius.course.entities.StudentProgress;
import org.darius.course.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentProgressRepository extends JpaRepository<StudentProgress, Long> {

    Optional<StudentProgress> findByStudentIdAndSemester_Id(
            String studentId, Long semesterId
    );

    List<StudentProgress> findBySemester_Id(Long semesterId);

    List<StudentProgress> findByStudentId(String studentId);

    List<StudentProgress> findBySemester_IdAndStatus(Long semesterId, ProgressStatus status);

    // Vérifier si tous les étudiants d'un semestre ont un StudentProgress
    @Query("""
        SELECT COUNT(DISTINCT e.studentId) = COUNT(DISTINCT sp.studentId)
        FROM Enrollment e
        LEFT JOIN StudentProgress sp
          ON sp.studentId = e.studentId
          AND sp.semester.id = e.semester.id
        WHERE e.semester.id = :semesterId
          AND e.status = 'ACTIVE'
        """)
    boolean allStudentsHaveProgress(@Param("semesterId") Long semesterId);

    // Progression d'une promotion entière pour le pipeline ML
    @Query("""
        SELECT sp FROM StudentProgress sp
        JOIN sp.semester s
        WHERE s.academicYear = :academicYear
        ORDER BY sp.semesterAverage DESC
        """)
    List<StudentProgress> findByAcademicYear(@Param("academicYear") String academicYear);
}