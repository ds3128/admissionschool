package org.darius.userservice.repositories;

import org.darius.userservice.entities.Student;
import org.darius.userservice.common.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {

    Optional<Student> findByProfileId(String profileId);

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByProfileId(String profileId);

    boolean existsByStudentNumber(String studentNumber);

    // Filtres combinés pour la liste paginée
    @Query("""
        SELECT s FROM Student s
        WHERE (:filiereId IS NULL OR s.filiere.id = :filiereId)
          AND (:levelId   IS NULL OR s.currentLevel.id = :levelId)
          AND (:status    IS NULL OR s.status = :status)
        """)
    Page<Student> findWithFilters(
            @Param("filiereId") Long filiereId,
            @Param("levelId")   Long levelId,
            @Param("status")    StudentStatus status,
            Pageable pageable
    );

    List<Student> findByFiliere_IdAndCurrentLevel_IdAndStatus(Long filiereId, Long currentLevelId, StudentStatus status);

    // Tous les étudiants actifs d'une filière
    List<Student> findByFiliere_IdAndStatus(Long filiereId, StudentStatus status);

    // Tous les étudiants d'un niveau
    List<Student> findByCurrentLevel_Id(Long levelId);

    // Comptage par filière et statut (utile pour les stats)
    long countByFiliere_IdAndStatus(Long filiereId, StudentStatus status);

    // Recherche par numéro matricule (partielle)
    @Query("""
        SELECT s FROM Student s
        WHERE LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Student> searchByStudentNumber(@Param("query") String query);
}