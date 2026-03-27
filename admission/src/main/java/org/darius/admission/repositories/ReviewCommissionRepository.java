package org.darius.admission.repositories;

import org.darius.admission.entities.ReviewCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewCommissionRepository extends JpaRepository<ReviewCommission, Long> {

    Optional<ReviewCommission> findByOffer_Id(Long offerId);

    boolean existsByOffer_Id(Long offerId);
}