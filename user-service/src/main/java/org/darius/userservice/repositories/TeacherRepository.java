package org.darius.userservice.repositories;

import org.darius.userservice.entities.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, String> {

    Optional<Teacher> findByProfileId(String profileId);

    Optional<Teacher> findByUserId(String userId);

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);

    boolean existsByProfileId(String profileId);

    boolean existsByUserId(String userId);

    List<Teacher> findByDepartment_IdAndActive(Long departmentId, boolean active);

    // Liste paginée avec filtres
    @Query("""
        SELECT t FROM Teacher t
        WHERE (:departmentId IS NULL OR t.department.id = :departmentId)
          AND (:active     IS NULL OR t.active = :active)
        """)
    Page<Teacher> findWithFilters(
            @Param("departmentId") Long departmentId,
            @Param("active")     Boolean active,
            Pageable pageable
    );

    // Recherche par numéro employé
    @Query("""
        SELECT t FROM Teacher t
        WHERE LOWER(t.employeeNumber) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<Teacher> searchByEmployeeNumber(@Param("query") String query);
}