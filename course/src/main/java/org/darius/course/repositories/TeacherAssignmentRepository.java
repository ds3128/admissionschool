package org.darius.course.repositories;

import org.darius.course.entities.TeacherAssignment;
import org.darius.course.enums.TeachingRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {

    List<TeacherAssignment> findByTeacherId(String teacherId);

    List<TeacherAssignment> findByTeacherIdAndSemester_Id(String teacherId, Long semesterId);

    List<TeacherAssignment> findByMatiere_Id(Long matiereId);

    List<TeacherAssignment> findByMatiere_IdAndSemester_Id(Long matiereId, Long semesterId);

    boolean existsByTeacherIdAndMatiere_IdAndRoleAndSemester_Id(
            String teacherId, Long matiereId, TeachingRole role, Long semesterId
    );

    // Vérifier si un enseignant est affecté à une matière (any role) pour un semestre
    @Query("""
        SELECT COUNT(ta) > 0 FROM TeacherAssignment ta
        WHERE ta.teacherId = :teacherId
          AND ta.matiere.id = :matiereId
          AND ta.semester.id = :semesterId
        """)
    boolean isTeacherAssignedToMatiere(
            @Param("teacherId") String teacherId,
            @Param("matiereId") Long matiereId,
            @Param("semesterId") Long semesterId
    );

    // Total heures assignées à un enseignant sur un semestre
    @Query("""
        SELECT COALESCE(SUM(ta.assignedHours), 0) FROM TeacherAssignment ta
        WHERE ta.teacherId = :teacherId
          AND ta.semester.id = :semesterId
        """)
    int sumAssignedHoursByTeacherAndSemester(
            @Param("teacherId") String teacherId,
            @Param("semesterId") Long semesterId
    );
}