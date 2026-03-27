package org.darius.course.repositories;

import org.darius.course.entities.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {

    Optional<Matiere> findByCode(String code);

    boolean existsByCode(String code);

    List<Matiere> findByTeachingUnit_Id(Long teachingUnitId);

    // Toutes les matières d'un niveau via les UEs
    @Query("""
        SELECT m FROM Matiere m
        JOIN m.teachingUnit tu
        WHERE tu.studyLevelId = :levelId
        """)
    List<Matiere> findByStudyLevelId(@Param("levelId") Long levelId);

    // Matières d'un niveau pour un semestre donné (numéro de semestre dans l'UE)
    @Query("""
        SELECT m FROM Matiere m
        JOIN m.teachingUnit tu
        WHERE tu.studyLevelId = :levelId
          AND tu.semesterNumber = :semesterNumber
        """)
    List<Matiere> findByStudyLevelIdAndSemesterNumber(
            @Param("levelId") Long levelId,
            @Param("semesterNumber") int semesterNumber
    );
}
