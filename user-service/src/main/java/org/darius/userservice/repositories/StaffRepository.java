package org.darius.userservice.repositories;

import org.darius.userservice.entities.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, String> {

    Optional<Staff> findByProfileId(String profileId);

    Optional<Staff> findByUserId(String userId);

    Optional<Staff> findByStaffNumber(String staffNumber);

    boolean existsByProfileId(String profileId);

    boolean existsByUserId(String userId);

    // Liste paginée avec filtres
    @Query("""
        SELECT s FROM Staff s
        WHERE (:departmentId IS NULL OR s.department.id = :departmentId)
          AND (:active     IS NULL OR s.active = :active)
        """)
    Page<Staff> findWithFilters(
            @Param("departmentId") Long departmentId,
            @Param("active")     Boolean active,
            Pageable pageable
    );
}