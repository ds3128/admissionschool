package org.darius.payment.repositories;

import org.darius.payment.common.enums.ScholarshipSource;
import org.darius.payment.entities.Scholarship;
import org.darius.payment.common.enums.ScholarshipStatus;
import org.darius.payment.common.enums.ScholarshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScholarshipRepository extends JpaRepository<Scholarship, Long> {

    List<Scholarship> findByStudentId(String studentId);

    // Vérifier unicité par étudiant/année/type
    boolean existsByStudentIdAndAcademicYearAndType(
            String studentId, String academicYear, ScholarshipType type
    );

    // Bourse active d'un étudiant pour une année
    Optional<Scholarship> findByStudentIdAndAcademicYearAndStatus(
            String studentId, String academicYear, ScholarshipStatus status
    );

    // Toutes les bourses actives d'un type — pour renouvellement
    List<Scholarship> findByTypeAndStatus(ScholarshipType type, ScholarshipStatus status);

    // Toutes les bourses actives — pour déduction sur factures
    List<Scholarship> findByStudentIdAndStatus(String studentId, ScholarshipStatus status);

    // Stats
    @Query("SELECT COUNT(s) FROM Scholarship s WHERE s.status = 'ACTIVE'")
    long countActiveScholarships();

    @Query("SELECT SUM(s.amount) FROM Scholarship s WHERE s.status = 'ACTIVE'")
    BigDecimal sumActiveScholarshipAmounts();

    // Bourses actives d'un étudiant filtrées par source — pour les disbursements
    List<Scholarship> findByStudentIdAndStatusAndSource(
            String studentId,
            ScholarshipStatus status,
            ScholarshipSource source
    );

    // Dans ScholarshipRepository
    List<Scholarship> findByStudentIdInAndStatus(List<String> studentIds, ScholarshipStatus status);

    Set<Scholarship> findByStatus(ScholarshipStatus status);
}