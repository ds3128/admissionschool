package org.darius.admission.repositories;

import org.darius.admission.common.enums.DocumentType;
import org.darius.admission.entities.RequiredDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequiredDocumentRepository extends JpaRepository<RequiredDocument, Long> {

    List<RequiredDocument> findByOffer_Id(Long offerId);

    List<RequiredDocument> findByOffer_IdAndIsMandatory(Long offerId, boolean isMandatory);

    boolean existsByOffer_IdAndDocumentType(Long offerId, DocumentType documentType);
}