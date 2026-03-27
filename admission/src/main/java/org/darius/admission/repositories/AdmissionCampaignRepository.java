package org.darius.admission.repositories;

import org.darius.admission.common.enums.CampaignStatus;
import org.darius.admission.entities.AdmissionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdmissionCampaignRepository extends JpaRepository<AdmissionCampaign, Long> {

    Optional<AdmissionCampaign> findByAcademicYear(String academicYear);

    boolean existsByAcademicYear(String academicYear);

    // Campagne actuellement ouverte
    Optional<AdmissionCampaign> findByStatus(CampaignStatus status);

    // Campagnes dont la date d'ouverture est dépassée mais encore UPCOMING
    // → utilisé par le scheduler pour passer à OPEN
    List<AdmissionCampaign> findByStatusAndStartDateLessThanEqual(
            CampaignStatus status, LocalDate date
    );

    // Campagnes dont la date de clôture est dépassée mais encore OPEN
    // → utilisé par le scheduler pour passer à CLOSED
    List<AdmissionCampaign> findByStatusAndEndDateLessThan(
            CampaignStatus status, LocalDate date
    );
}