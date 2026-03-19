package org.darius.userservice.repositories;

import org.darius.userservice.entities.Filiere;
import org.darius.userservice.common.enums.FiliereStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FiliereRepository extends JpaRepository<Filiere, Long> {

    Optional<Filiere> findByCode(String code);

    boolean existsByCode(String code);

    // Filières par département et statut
    List<Filiere> findByDepartment_IdAndStatus(Long departmentId, FiliereStatus status);

    // Filières par statut uniquement
    List<Filiere> findByStatus(FiliereStatus status);

    // Filières actives (pour l'Admission Service via HTTP)
    List<Filiere> findByStatusOrderByName(FiliereStatus status);

    // Vérifie si une filière a des étudiants actifs (avant archivage)
    @Query("""
        SELECT COUNT(s) > 0 FROM Student s
        WHERE s.filiere.id = :filiereId
          AND s.status = 'ACTIVE'
        """)
    boolean hasActiveStudents(@Param("filiereId") Long filiereId);
}