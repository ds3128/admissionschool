package org.darius.admission.repositories;

import org.darius.admission.entities.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByApplication_Id(String applicationId);
}