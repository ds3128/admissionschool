package org.darius.admission.repositories;

import org.darius.admission.common.enums.DocumentType;
import org.darius.admission.entities.DossierDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DossierDocumentRepository extends JpaRepository<DossierDocument, Long> {

    List<DossierDocument> findByDossier_Id(Long dossierId);

    Optional<DossierDocument> findByDossier_IdAndType(Long dossierId, DocumentType type);

    boolean existsByDossier_IdAndType(Long dossierId, DocumentType type);

    // Vérifie si tous les documents obligatoires sont présents et validés
    @Query("""
        SELECT COUNT(rd) = 0 FROM RequiredDocument rd
        WHERE rd.offer.id = :offerId
          AND rd.isMandatory = true
          AND NOT EXISTS (
              SELECT d FROM DossierDocument d
              WHERE d.dossier.id = :dossierId
                AND d.type = rd.documentType
                AND d.status != 'REJECTED'
          )
        """)
    boolean allMandatoryDocumentsPresent(
            @Param("dossierId") Long dossierId,
            @Param("offerId") Long offerId
    );
}