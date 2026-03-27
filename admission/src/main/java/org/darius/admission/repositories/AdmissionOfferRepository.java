package org.darius.admission.repositories;

import org.darius.admission.common.enums.OfferLevel;
import org.darius.admission.common.enums.OfferStatus;
import org.darius.admission.entities.AdmissionOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdmissionOfferRepository extends JpaRepository<AdmissionOffer, Long> {

    // Toutes les offres d'une campagne
    List<AdmissionOffer> findByCampaign_Id(Long campaignId);

    // Offres ouvertes d'une campagne (pour les candidats)
    List<AdmissionOffer> findByCampaign_IdAndStatus(Long campaignId, OfferStatus status);

    // Offres ouvertes d'une campagne filtrées par niveau
    List<AdmissionOffer> findByCampaign_IdAndStatusAndLevel(
            Long campaignId, OfferStatus status, OfferLevel level
    );

    // Offre d'une filière pour une campagne
    Optional<AdmissionOffer> findByCampaign_IdAndFiliereId(Long campaignId, Long filiereId);

    // Offres dont la deadline est dépassée mais encore OPEN
    // → utilisé par le scheduler
    @Query("""
        SELECT o FROM AdmissionOffer o
        WHERE o.status = 'OPEN'
          AND o.deadline < :today
        """)
    List<AdmissionOffer> findExpiredOpenOffers(@Param("today") LocalDate today);

    // Offres pleines (acceptedCount >= maxCapacity) — vérification pour passage à FULL
    @Query("""
        SELECT o FROM AdmissionOffer o
        WHERE o.status = 'OPEN'
          AND o.acceptedCount >= o.maxCapacity
        """)
    List<AdmissionOffer> findFullOffers();
}