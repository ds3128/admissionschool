package org.darius.admission.services;

import org.darius.admission.common.dtos.requests.*;
import org.darius.admission.common.dtos.responses.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationService {

    /**
     * Crée une candidature en DRAFT.
     * Vérifie qu'une campagne OPEN existe.
     * Crée le Dossier et le CandidateProfile vides.
     * Un candidat ne peut avoir qu'une candidature par campagne.
     */
    ApplicationResponse createApplication(String userId, CreateApplicationRequest request);

    /** Retourne toutes les candidatures de l'utilisateur connecté. */
    List<ApplicationResponse> getMyApplications(String userId);

    /** Retourne une candidature par son ID (candidat — vérifie la propriété). */
    ApplicationResponse getApplicationById(String applicationId, String userId);

    /**
     * Met à jour le CandidateProfile (données académiques).
     * Uniquement possible si dossier non verrouillé.
     */
    CandidateProfileResponse updateCandidateProfile(
            String applicationId,
            String userId,
            UpdateCandidateProfileRequest request
    );

    /**
     * Ajoute un choix de formation.
     * Vérifie deadline, disponibilité, doublon, max choix.
     * Incrémente AdmissionOffer.currentCount.
     */
    ChoiceResponse addChoice(String applicationId, String userId, AddChoiceRequest request);

    /** Supprime un choix (DRAFT uniquement). Décrémente currentCount. */
    void removeChoice(String applicationId, Long choiceId, String userId);

    /** Réordonne les choix (DRAFT uniquement). */
    ApplicationResponse reorderChoices(
            String applicationId,
            String userId,
            ReorderChoicesRequest request
    );

    /**
     * Upload un document dans le dossier.
     * Vérifie format (PDF, JPEG, PNG) et taille.
     * Recalcule isComplete après upload.
     */
    DocumentResponse uploadDocument(
            String applicationId,
            String userId,
            MultipartFile file,
            String documentType
    );

    /** Supprime un document (dossier non verrouillé uniquement). */
    void removeDocument(String applicationId, Long documentId, String userId);

    /**
     * Initie le paiement des frais de dossier.
     * Crée un AdmissionPayment en PENDING.
     */
    PaymentResponse initiatePayment(String applicationId, String userId);

    /**
     * Soumet la candidature.
     * Vérifie paiement COMPLETED et isComplete.
     * Re-vérifie les deadlines — retire les choix expirés.
     * Récupère les données UserProfile depuis le User Service.
     * Verrouille le dossier. Publie ApplicationSubmitted.
     */
    ApplicationResponse submitApplication(String applicationId, String userId);

    /**
     * Confirme un choix parmi les choix ACCEPTED.
     * Retire les autres choix acceptés.
     * Publie ApplicationAccepted.
     */
    ApplicationResponse confirmChoice(
            String applicationId,
            String userId,
            ConfirmChoiceRequest request
    );

    /** Retire une candidature (DRAFT uniquement). */
    void withdrawApplication(String applicationId, String userId);

    /** Retourne le statut de confirmation (choix acceptés + délai restant). */
    ConfirmationResponse getConfirmationStatus(String applicationId, String userId);
}