package org.darius.userservice.repositories;

import org.darius.userservice.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    // Vérifie si un département possède des filières actives
    // (utile avant suppression)
    @Query("""
        SELECT COUNT(f) > 0 FROM Filiere f
        WHERE f.department.id = :departmentId
          AND f.status = 'ACTIVE'
        """)
    boolean hasActiveFilieres(@Param("departmentId") Long departmentId);

    // Vérifie si un département possède des enseignants actifs
    @Query("""
        SELECT COUNT(t) > 0 FROM Teacher t
        WHERE t.department.id = :departmentId
          AND t.active = true
        """)
    boolean hasActiveTeachers(@Param("departmentId") Long departmentId);
}