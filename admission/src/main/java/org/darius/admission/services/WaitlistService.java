package org.darius.admission.services;


import org.darius.admission.common.dtos.responses.WaitlistEntryResponse;

import java.util.List;

public interface WaitlistService {

    /**
     * Retourne la liste d'attente d'une offre dans l'ordre du rang.
     */
    List<WaitlistEntryResponse> getWaitlistByOffer(Long offerId);

    /**
     * Promotionne le premier candidat en attente quand une place se libère.
     * WaitlistEntry → PROMOTED.
     * expiresAt = now + 48h.
     * Publie WaitlistPromoted.
     */
    void promoteNextCandidate(Long offerId);

    /**
     * Job schedulé — expire les promotions dont le délai 48h est dépassé.
     * WaitlistEntry → EXPIRED.
     * Déclenche promoteNextCandidate automatiquement.
     */
    void processExpiredPromotions();
}