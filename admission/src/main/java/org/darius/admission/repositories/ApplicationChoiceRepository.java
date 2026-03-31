package org.darius.admission.repositories;

import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.entities.ApplicationChoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationChoiceRepository extends JpaRepository<ApplicationChoice, Long> {

    // Tous les choix d'une candidature
    List<ApplicationChoice> findByApplication_IdOrderByChoiceOrder(String applicationId);

    // Choix actifs (non retirés) d'une candidature
    @Query("""
        SELECT c FROM ApplicationChoice c
        WHERE c.application.id = :applicationId
          AND c.status != 'WITHDRAWN'
        ORDER BY c.choiceOrder
        """)
    List<ApplicationChoice> findActiveChoices(@Param("applicationId") String applicationId);

    // Vérification doublon : candidat a déjà choisi cette offre
    boolean existsByApplication_IdAndOffer_Id(String applicationId, Long offerId);

    // Compte des choix actifs d'une candidature
    @Query("""
        SELECT COUNT(c) FROM ApplicationChoice c
        WHERE c.application.id = :applicationId
          AND c.status != 'WITHDRAWN'
        """)
    long countActiveChoices(@Param("applicationId") String applicationId);

    // Choix acceptés d'une candidature
    List<ApplicationChoice> findByApplication_IdAndStatus(
            String applicationId, ChoiceStatus status
    );

    // Choix en attente de décision (pour la commission)
    @Query("""
        SELECT c FROM ApplicationChoice c
        WHERE c.offer.id = :offerId
          AND c.status IN ('PENDING_COMMISSION', 'UNDER_COMMISSION_REVIEW')
        ORDER BY c.application.submittedAt
        """)
    List<ApplicationChoice> findPendingChoicesByOffer(@Param("offerId") Long offerId);

    // Vérification : tous les choix d'une candidature ont une décision finale
    @Query("""
        SELECT COUNT(c) = 0 FROM ApplicationChoice c
        WHERE c.application.id = :applicationId
          AND c.status NOT IN (
            'ACCEPTED', 'REJECTED', 'CONFIRMED', 'WITHDRAWN',
            'WAITLISTED', 'PROMOTED_FROM_WAITLIST'
          )
        """)
    boolean allChoicesHaveFinalStatus(@Param("applicationId") String applicationId);

    // Choix avec priorité la plus haute (order = 1) en statut ACCEPTED
    Optional<ApplicationChoice> findFirstByApplication_IdAndStatusOrderByChoiceOrder(
            String applicationId, ChoiceStatus status
    );

    long countByOffer_IdAndStatus(Long offerId, ChoiceStatus status);

    List<ApplicationChoice> findByOffer_IdAndStatusOrderByWaitlistEntryAsc(Long offerId, ChoiceStatus status);
}