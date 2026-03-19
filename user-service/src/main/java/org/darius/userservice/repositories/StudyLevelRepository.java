package org.darius.userservice.repositories;

import org.darius.userservice.entities.StudyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyLevelRepository extends JpaRepository<StudyLevel, Long> {

    // Tous les niveaux d'une filière dans l'ordre
    List<StudyLevel> findByFiliere_IdOrderByOrder(Long filiereId);

    // Premier niveau d'une filière (order = 1)
    Optional<StudyLevel> findByFiliere_IdAndOrder(Long filiereId, int order);

    // Nombre de niveaux dans une filière
    int countByFiliere_Id(Long filiereId);

    // Prochain niveau (order + 1 dans la même filière)
    @Query("""
        SELECT sl FROM StudyLevel sl
        WHERE sl.filiere.id = (
            SELECT sl2.filiere.id FROM StudyLevel sl2 WHERE sl2.id = :currentLevelId
        )
        AND sl.order = (
            SELECT sl2.order + 1 FROM StudyLevel sl2 WHERE sl2.id = :currentLevelId
        )
        """)
    Optional<StudyLevel> findNextLevel(@Param("currentLevelId") Long currentLevelId);

    // Dernier niveau d'une filière (order max)
    @Query("""
        SELECT sl FROM StudyLevel sl
        WHERE sl.filiere.id = :filiereId
          AND sl.order = (
              SELECT MAX(sl2.order) FROM StudyLevel sl2 WHERE sl2.filiere.id = :filiereId
          )
        """)
    Optional<StudyLevel> findLastLevel(@Param("filiereId") Long filiereId);

    // Vérifie si un niveau est le dernier de sa filière
    @Query("""
        SELECT CASE WHEN sl.order = (
            SELECT MAX(sl2.order) FROM StudyLevel sl2 WHERE sl2.filiere.id = sl.filiere.id
        ) THEN true ELSE false END
        FROM StudyLevel sl WHERE sl.id = :levelId
        """)
    boolean isLastLevel(@Param("levelId") Long levelId);
}