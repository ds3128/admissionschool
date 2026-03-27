package org.darius.course.repositories;

import org.darius.course.entities.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByMatiere_IdAndSemester_Id(Long matiereId, Long semesterId);

    List<Evaluation> findByMatiere_IdAndSemester_IdAndIsPublished(
            Long matiereId, Long semesterId, boolean isPublished
    );

    // Somme des coefficients d'une matière pour un semestre
    @Query("""
        SELECT COALESCE(SUM(e.coefficient), 0.0) FROM Evaluation e
        WHERE e.matiere.id = :matiereId
          AND e.semester.id = :semesterId
        """)
    double sumCoefficientsByMatiereAndSemester(
            @Param("matiereId") Long matiereId,
            @Param("semesterId") Long semesterId
    );

    // Évaluations à venir pour une matière
    @Query("""
        SELECT e FROM Evaluation e
        WHERE e.matiere.id = :matiereId
          AND e.semester.id = :semesterId
          AND e.date >= :today
        ORDER BY e.date
        """)
    List<Evaluation> findUpcomingByMatiereAndSemester(
            @Param("matiereId") Long matiereId,
            @Param("semesterId") Long semesterId,
            @Param("today") LocalDate today
    );

    // Évaluations publiées d'une matière
    List<Evaluation> findByMatiere_IdAndSemester_IdAndIsPublishedTrue(
            Long matiereId, Long semesterId
    );
}
