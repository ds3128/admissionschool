package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.AdminReviewRequest;
import org.darius.admission.common.dtos.requests.RequestAdditionalDocsRequest;
import org.darius.admission.common.dtos.responses.ApplicationResponse;
import org.darius.admission.common.dtos.responses.ApplicationSummaryResponse;
import org.darius.admission.common.dtos.responses.PageResponse;
import org.darius.admission.common.enums.ApplicationStatus;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.entities.Application;
import org.darius.admission.entities.ApplicationStatusHistory;
import org.darius.admission.entities.CandidateProfile;
import org.darius.admission.evens.published.ApplicationAdminReviewEvent;
import org.darius.admission.evens.published.ApplicationPendingCommissionEvent;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.*;
import org.darius.admission.services.AdminApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminApplicationServiceImpl implements AdminApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final DossierRepository dossierRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final CandidateProfileRepository profileRepository;
    private final AdmissionEventProducer eventProducer;
    private final AdmissionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummaryResponse> getAllApplications(
            ApplicationStatus status, Long campaignId, int page, int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        Page<Application> applications = applicationRepository.findWithFilters(
                status, campaignId, pageable
        );

        List<ApplicationSummaryResponse> content = applications.getContent().stream()
                .map(app -> {
                    CandidateProfile profile = profileRepository
                            .findByApplication_Id(app.getId()).orElse(null);
                    return mapper.toApplicationSummaryResponse(app, profile);
                })
                .toList();

        return PageResponse.<ApplicationSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(applications.getTotalElements())
                .totalPages(applications.getTotalPages())
                .last(applications.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByIdAdmin(String applicationId) {
        Application app = findOrThrow(applicationId);
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse processAdminReview(
            String applicationId, String adminUserId, AdminReviewRequest request
    ) {
        Application app = findOrThrow(applicationId);

        if (app.getStatus() != ApplicationStatus.SUBMITTED
                && app.getStatus() != ApplicationStatus.ADDITIONAL_DOCS_REQUIRED) {
            throw new InvalidOperationException(
                    "La candidature doit être en statut SUBMITTED ou ADDITIONAL_DOCS_REQUIRED"
            );
        }

        // Prise en charge → UNDER_ADMIN_REVIEW
        changeStatus(app, ApplicationStatus.UNDER_ADMIN_REVIEW, adminUserId,
                "Prise en charge administrative");
        app = applicationRepository.save(app);

        CandidateProfile profile = profileRepository
                .findByApplication_Id(applicationId).orElse(null);

        if (request.isApproved()) {
            // Approuvé → PENDING_COMMISSION
            changeStatus(app, ApplicationStatus.PENDING_COMMISSION, adminUserId,
                    request.getComment() != null ? request.getComment() : "Dossier conforme");

            // Passer tous les choix actifs en PENDING_COMMISSION
            Application finalApp = app;
            choiceRepository.findActiveChoices(applicationId).forEach(c -> {
                c.setStatus(ChoiceStatus.PENDING_COMMISSION);
                choiceRepository.save(c);

                // Publier un event pour chaque choix
                eventProducer.publishApplicationPendingCommission(
                        ApplicationPendingCommissionEvent.builder()
                                .applicationId(applicationId)
                                .userId(finalApp.getUserId())
                                .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                                .offerId(c.getOffer().getId())
                                .filiereName(c.getFiliereName())
                                .build()
                );
            });

        } else {
            // Non conforme → ADDITIONAL_DOCS_REQUIRED
            changeStatus(app, ApplicationStatus.ADDITIONAL_DOCS_REQUIRED, adminUserId,
                    request.getComment() != null ? request.getComment() : "Documents non conformes");

            // Déverrouiller le dossier
            dossierRepository.findByApplication_Id(applicationId).ifPresent(d -> {
                d.setLocked(false);
                d.setUnlockReason(request.getComment());
                dossierRepository.save(d);
            });

            // Notifier le candidat
            eventProducer.publishApplicationAdminReview(
                    ApplicationAdminReviewEvent.builder()
                            .applicationId(applicationId)
                            .userId(app.getUserId())
                            .personalEmail(profile != null ? profile.getPersonalEmail() : null)
                            .approved(false)
                            .comment(request.getComment())
                            .build()
            );
        }

        app = applicationRepository.save(app);
        log.info("Révision admin : applicationId={}, approved={}", applicationId, request.isApproved());
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse requestAdditionalDocuments(
            String applicationId, String adminUserId, RequestAdditionalDocsRequest request
    ) {
        Application app = findOrThrow(applicationId);

        changeStatus(app, ApplicationStatus.ADDITIONAL_DOCS_REQUIRED, adminUserId, request.getReason());

        dossierRepository.findByApplication_Id(applicationId).ifPresent(d -> {
            d.setLocked(false);
            d.setUnlockReason(request.getReason());
            dossierRepository.save(d);
        });

        app = applicationRepository.save(app);
        return mapper.toApplicationResponse(app);
    }

    @Override
    @Transactional
    public ApplicationResponse forwardToCommission(String applicationId, String adminUserId) {
        Application app = findOrThrow(applicationId);

        if (app.getStatus() != ApplicationStatus.UNDER_ADMIN_REVIEW) {
            throw new InvalidOperationException(
                    "La candidature doit être en cours de révision administrative"
            );
        }

        changeStatus(app, ApplicationStatus.PENDING_COMMISSION, adminUserId,
                "Transmis manuellement à la commission");

        choiceRepository.findActiveChoices(applicationId).forEach(c -> {
            c.setStatus(ChoiceStatus.PENDING_COMMISSION);
            choiceRepository.save(c);
        });

        app = applicationRepository.save(app);
        return mapper.toApplicationResponse(app);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Application findOrThrow(String id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature introuvable : id=" + id));
    }

    private void changeStatus(
            Application app, ApplicationStatus newStatus,
            String changedBy, String comment
    ) {
        ApplicationStatusHistory history = ApplicationStatusHistory.builder()
                .application(app)
                .fromStatus(app.getStatus())
                .toStatus(newStatus)
                .changedBy(changedBy)
                .comment(comment)
                .build();
        historyRepository.save(history);
        app.setStatus(newStatus);
        app.setLastStatusChange(LocalDateTime.now());
    }
}