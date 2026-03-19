package org.darius.userservice.repositories;

import org.darius.userservice.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    Optional<UserProfile> findByPersonalEmail(String personalEmail);

    boolean existsByUserId(String userId);

    boolean existsByPersonalEmail(String personalEmail);

    // Recherche globale insensible à la casse sur prénom + nom + email
    @Query("""
        SELECT p FROM UserProfile p
        WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.personalEmail) LIKE LOWER(CONCAT('%', :query, '%'))
        """)
    List<UserProfile> searchByQuery(@Param("query") String query);
}