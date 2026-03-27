package org.darius.admission.repositories;

import org.darius.admission.entities.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier, Long> {

    Optional<Dossier> findByApplication_Id(String applicationId);
}