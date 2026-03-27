package org.darius.course.repositories;

import org.darius.course.entities.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByStudentIdAndEvaluation_Id(String studentId, Long evaluationId);

    List<Grade> findByStudentIdAndSemester_Id(String studentId, Long semesterId);

    List<Grade> findByStudentIdAndMatiere_IdAndSemester_Id(
            String studentId, Long matiereId, Long semesterId
    );

    List<Grade> findByEvaluation_Id(Long evaluationId);

    List<Grade> findByMatiere_IdAndSemester_Id(Long matiereId, Long semesterId);

    boolean existsByStudentIdAndEvaluation_Id(String studentId, Long evaluationId);

    // Notes d'un étudiant pour toutes les matières d'un semestre (pour calcul progression)
    @Query("""
        SELECT g FROM Grade g
        WHERE g.studentId = :studentId
          AND g.semester.id = :semesterId
        ORDER BY g.matiere.id, g.gradedAt
        """)
    List<Grade> findAllByStudentAndSemester(
            @Param("studentId") String studentId,
            @Param("semesterId") Long semesterId
    );

    // Toutes les notes d'un semestre — pour calcul global des progressions
    @Query("""
        SELECT g FROM Grade g
        WHERE g.semester.id = :semesterId
        ORDER BY g.studentId, g.matiere.id
        """)
    List<Grade> findAllBySemester(@Param("semesterId") Long semesterId);

    // Stats pour ClassStats — min/max/avg d'une évaluation
    @Query("""
    SELECT MIN(g.score), MAX(g.score), AVG(g.score), COUNT(g)
    FROM Grade g
    WHERE g.evaluation.id = :evaluationId
    """)
    List<Object[]> computeStatsForEvaluation(@Param("evaluationId") Long evaluationId);
}
