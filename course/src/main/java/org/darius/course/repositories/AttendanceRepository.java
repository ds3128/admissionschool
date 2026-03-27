package org.darius.course.repositories;

import org.darius.course.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySession_Id(Long sessionId);

    List<Attendance> findByStudentId(String studentId);

    Optional<Attendance> findBySession_IdAndStudentId(Long sessionId, String studentId);

    // Toutes les présences d'un étudiant pour une matière et un semestre
    @Query("""
        SELECT a FROM Attendance a
        JOIN a.session s
        JOIN s.plannedSlot ps
        WHERE a.studentId = :studentId
          AND ps.matiere.id = :matiereId
          AND ps.semester.id = :semesterId
        """)
    List<Attendance> findByStudentAndMatiereAndSemester(
            @Param("studentId") String studentId,
            @Param("matiereId") Long matiereId,
            @Param("semesterId") Long semesterId
    );

    // Comptage présences/absences par étudiant pour une matière
    @Query("""
        SELECT COUNT(a) FROM Attendance a
        JOIN a.session s
        JOIN s.plannedSlot ps
        WHERE a.studentId = :studentId
          AND ps.matiere.id = :matiereId
          AND ps.semester.id = :semesterId
          AND a.present = :present
        """)
    long countByStudentAndMatiereAndSemesterAndPresent(
            @Param("studentId") String studentId,
            @Param("matiereId") Long matiereId,
            @Param("semesterId") Long semesterId,
            @Param("present") boolean present
    );
}
