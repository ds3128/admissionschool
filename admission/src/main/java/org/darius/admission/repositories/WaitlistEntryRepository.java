package org.darius.admission.repositories;

import org.darius.admission.common.enums.WaitlistStatus;
import org.darius.admission.entities.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByChoice_Id(Long choiceId);

    // Liste d'attente d'une offre dans l'ordre du rang
    List<WaitlistEntry> findByOfferIdAndStatusOrderByRank(Long offerId, WaitlistStatus status);

    // Premier candidat en attente d'une offre (rang le plus bas)
    Optional<WaitlistEntry> findFirstByOfferIdAndStatusOrderByRank(
            Long offerId, WaitlistStatus status
    );

    // Prochain rang à attribuer
    @Query("""
        SELECT COALESCE(MAX(w.rank), 0) + 1
        FROM WaitlistEntry w
        WHERE w.offerId = :offerId
          AND w.status IN ('WAITING', 'PROMOTED')
        """)
    int getNextRank(@Param("offerId") Long offerId);

    // Promotions expirées (délai 48h dépassé)
    @Query("""
        SELECT w FROM WaitlistEntry w
        WHERE w.status = 'PROMOTED'
          AND w.expiresAt < :now
        """)
    List<WaitlistEntry> findExpiredPromotions(@Param("now") LocalDateTime now);
}