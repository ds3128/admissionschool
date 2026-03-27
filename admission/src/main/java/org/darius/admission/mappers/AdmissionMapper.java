package org.darius.admission.mappers;

import org.darius.admission.common.dtos.responses.*;
import org.darius.admission.entities.*;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AdmissionMapper {

    // ── Campaign ──────────────────────────────────────────────────────────────

    @Mapping(target = "offerCount", expression = "java(campaign.getOffers().size())")
    CampaignResponse toCampaignResponse(AdmissionCampaign campaign);


    // ── Offer ─────────────────────────────────────────────────────────────────

    @Mapping(target = "campaignId",      source = "campaign.id")
    @Mapping(target = "academicYear",    source = "campaign.academicYear")
    @Mapping(target = "availablePlaces", expression = "java(offer.getMaxCapacity() - offer.getAcceptedCount())")
    @Mapping(target = "requiredDocuments", source = "requiredDocuments")
    OfferResponse toOfferResponse(AdmissionOffer offer);

    @Mapping(target = "availablePlaces", expression = "java(offer.getMaxCapacity() - offer.getAcceptedCount())")
    OfferSummaryResponse toOfferSummaryResponse(AdmissionOffer offer);

    RequiredDocumentResponse toRequiredDocumentResponse(RequiredDocument doc);

    List<RequiredDocumentResponse> toRequiredDocumentResponseList(List<RequiredDocument> docs);


    // ── Application ───────────────────────────────────────────────────────────

    @Mapping(target = "campaignId",    source = "campaign.id")
    @Mapping(target = "choices",       source = "choices")
    @Mapping(target = "dossier",       source = "dossier")
    @Mapping(target = "candidateProfile", source = "candidateProfile")
    @Mapping(target = "payment",       source = "payment")
    @Mapping(target = "confirmationRequest", source = "confirmationRequest")
    @Mapping(target = "statusHistory", source = "statusHistory")
    ApplicationResponse toApplicationResponse(Application application);

    @Mapping(target = "id",                source = "application.id")
    @Mapping(target = "academicYear",      source = "application.academicYear")
    @Mapping(target = "status",            source = "application.status")
    @Mapping(target = "submittedAt",       source = "application.submittedAt")
    @Mapping(target = "createdAt",         source = "application.createdAt")
    @Mapping(target = "candidateFirstName", source = "profile.firstName")
    @Mapping(target = "candidateLastName",  source = "profile.lastName")
    @Mapping(target = "candidateEmail",     source = "profile.personalEmail")
    @Mapping(target = "choiceCount",
            expression = "java(application.getChoices() != null ? application.getChoices().size() : 0)")
    ApplicationSummaryResponse toApplicationSummaryResponse(
            Application application,
            CandidateProfile profile
    );


    // ── Choice ────────────────────────────────────────────────────────────────

    @Mapping(target = "offerId",       source = "offer.id")
    @Mapping(target = "waitlistEntry", source = "waitlistEntry")
    @Mapping(target = "interview",     source = "interview")
    ChoiceResponse toChoiceResponse(ApplicationChoice choice);


    // ── CandidateProfile ──────────────────────────────────────────────────────

    CandidateProfileResponse toCandidateProfileResponse(CandidateProfile profile);


    // ── Dossier ───────────────────────────────────────────────────────────────

    @Mapping(target = "documents", source = "documents")
    DossierResponse toDossierResponse(Dossier dossier);

    @Mapping(target = "fileUrl", source = "fileUrl")
    DocumentResponse toDocumentResponse(DossierDocument doc);


    // ── Payment ───────────────────────────────────────────────────────────────

    @Mapping(target = "id",  source = "id")
    PaymentResponse toPaymentResponse(AdmissionPayment payment);


    // ── Confirmation ──────────────────────────────────────────────────────────

    @Mapping(target = "remainingHours",
            expression = "java(computeRemainingHours(confirmation.getExpiresAt()))")
    ConfirmationResponse toConfirmationResponse(ConfirmationRequest confirmation);

    default Long computeRemainingHours(LocalDateTime expiresAt) {
        if (expiresAt == null) return null;
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), expiresAt);
        return Math.max(0, hours);
    }


    // ── StatusHistory ─────────────────────────────────────────────────────────

    StatusHistoryResponse toStatusHistoryResponse(ApplicationStatusHistory history);


    // ── Commission ────────────────────────────────────────────────────────────

    @Mapping(target = "offerId",     source = "offer.id")
    @Mapping(target = "filiereName", source = "offer.filiereName")
    @Mapping(target = "members",     source = "members")
    CommissionResponse toCommissionResponse(ReviewCommission commission);

    CommissionMemberResponse toCommissionMemberResponse(CommissionMember member);


    // ── Vote ──────────────────────────────────────────────────────────────────

    @Mapping(target = "choiceId", source = "choice.id")
    VoteResponse toVoteResponse(CommissionVote vote);


    // ── Interview ─────────────────────────────────────────────────────────────

    // Notes exclues volontairement — confidentielles
    @Mapping(target = "interviewerIds", source = "interviewers")
    InterviewResponse toInterviewResponse(Interview interview);


    // ── WaitlistEntry ─────────────────────────────────────────────────────────

    @Mapping(target = "remainingHours",
            expression = "java(computeRemainingHours(entry.getExpiresAt()))")
    WaitlistEntryResponse toWaitlistEntryResponse(WaitlistEntry entry);


    // ── ThesisApproval ────────────────────────────────────────────────────────

    @Mapping(target = "choiceId", source = "choice.id")
    ThesisApprovalResponse toThesisApprovalResponse(ThesisDirectorApproval approval);
}