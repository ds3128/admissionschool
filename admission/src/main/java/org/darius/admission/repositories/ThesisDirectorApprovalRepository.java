package org.darius.admission.repositories;

import org.darius.admission.common.enums.ApprovalStatus;
import org.darius.admission.entities.ThesisDirectorApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ThesisDirectorApprovalRepository
        extends JpaRepository<ThesisDirectorApproval, Long> {

    Optional<ThesisDirectorApproval> findByChoice_Id(Long choiceId);

    // Demandes en attente pour un directeur donné
    List<ThesisDirectorApproval> findByDirectorIdAndStatus(
            String directorId, ApprovalStatus status
    );

    // Demandes expirées encore en PENDING
    @Query("""
        SELECT t FROM ThesisDirectorApproval t
        WHERE t.status = 'PENDING'
          AND t.expiresAt < :now
        """)
    List<ThesisDirectorApproval> findExpiredPendingApprovals(
            @Param("now") LocalDateTime now
    );

    // Demandes à relancer (J+7 sans réponse)
    @Query("""
        SELECT t FROM ThesisDirectorApproval t
        WHERE t.status = 'PENDING'
          AND t.requestedAt <= :reminderDate
          AND t.expiresAt > :now
        """)
    List<ThesisDirectorApproval> findForReminder(
            @Param("reminderDate") LocalDateTime reminderDate,
            @Param("now")          LocalDateTime now
    );
}