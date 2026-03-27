package org.darius.course.repositories;

import org.darius.course.entities.Session;
import org.darius.course.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByMatiere_IdAndPlannedSlot_Semester_Id(Long matiereId, Long semesterId);

    List<Session> findByStatus(SessionStatus status);

    // Sessions de la semaine pour un groupe
    @Query("""
        SELECT s FROM Session s
        JOIN s.plannedSlot ps
        WHERE ps.group.id = :groupId
          AND s.date BETWEEN :from AND :to
        ORDER BY s.date, s.startTime
        """)
    List<Session> findByGroupAndWeek(
            @Param("groupId") Long groupId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // Sessions de la semaine pour un enseignant
    @Query("""
        SELECT s FROM Session s
        WHERE s.teacherId = :teacherId
          AND s.date BETWEEN :from AND :to
        ORDER BY s.date, s.startTime
        """)
    List<Session> findByTeacherAndWeek(
            @Param("teacherId") String teacherId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // Toutes les sessions d'une semaine (vue admin)
    @Query("""
        SELECT s FROM Session s
        WHERE s.date BETWEEN :from AND :to
        ORDER BY s.date, s.startTime
        """)
    List<Session> findByWeek(@Param("from") LocalDate from, @Param("to") LocalDate to);
}