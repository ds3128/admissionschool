package org.darius.admission.repositories;

import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.entities.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, String> {

    // Candidature d'un utilisateur pour une campagne — unicité
    Optional<Application> findByUserIdAndCampaign_Id(String userId, Long campaignId);

    boolean existsByUserIdAndCampaign_Id(String userId, Long campaignId);

    // Toutes les candidatures d'un utilisateur
    List<Application> findByUserId(String userId);

    // Liste paginée avec filtres (admin)
    @Query("""
        SELECT a FROM Application a
        WHERE (:status IS NULL OR a.status = :status)
          AND (:campaignId IS NULL OR a.campaign.id = :campaignId)
        """)
    Page<Application> findWithFilters(
            @Param("status") ApplicationStatus status,
            @Param("campaignId") Long campaignId,
            Pageable pageable
    );

    // Candidatures en attente de confirmation dont le délai a expiré
    // → utilisé par le scheduler de confirmation automatique
    @Query("""
        SELECT a FROM Application a
        JOIN a.confirmationRequest cr
        WHERE a.status = 'AWAITING_CONFIRMATION'
          AND cr.status = 'PENDING'
          AND cr.expiresAt < CURRENT_TIMESTAMP
        """)
    List<Application> findExpiredConfirmations();

    // Statistiques par campagne
    @Query("""
        SELECT a.status, COUNT(a)
        FROM Application a
        WHERE a.campaign.id = :campaignId
        GROUP BY a.status
        """)
    List<Object[]> countByStatusForCampaign(@Param("campaignId") Long campaignId);

    // Toutes les candidatures d'une campagne avec un statut donné
    List<Application> findByCampaign_IdAndStatus(Long campaignId, ApplicationStatus status);
}