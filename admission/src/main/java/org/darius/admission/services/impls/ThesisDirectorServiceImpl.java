package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.ThesisDirectorResponseRequest;
import org.darius.admission.common.dtos.responses.ThesisApprovalResponse;
import org.darius.admission.common.enums.ApprovalStatus;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.entities.ApplicationChoice;
import org.darius.admission.entities.ThesisDirectorApproval;
import org.darius.admission.exceptions.ForbiddenException;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.ApplicationChoiceRepository;
import org.darius.admission.repositories.ThesisDirectorApprovalRepository;
import org.darius.admission.services.ThesisDirectorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThesisDirectorServiceImpl implements ThesisDirectorService {

    private final ThesisDirectorApprovalRepository approvalRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final AdmissionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ThesisApprovalResponse> getPendingApprovals(String directorId) {
        return approvalRepository.findByDirectorIdAndStatus(directorId, ApprovalStatus.PENDING)
                .stream()
                .map(mapper::toThesisApprovalResponse)
                .toList();
    }

    @Override
    @Transactional
    public ThesisApprovalResponse respondToApproval(
            Long approvalId, String directorId, ThesisDirectorResponseRequest request
    ) {
        ThesisDirectorApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande d'accord introuvable : id=" + approvalId
                ));

        if (!approval.getDirectorId().equals(directorId)) {
            throw new ForbiddenException("Vous n'êtes pas le directeur concerné");
        }

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new InvalidOperationException(
                    "Cette demande n'est plus en attente"
            );
        }

        approval.setStatus(request.isApproved() ? ApprovalStatus.APPROVED : ApprovalStatus.REFUSED);
        approval.setComment(request.getComment());
        approval.setRespondedAt(LocalDateTime.now());
        approvalRepository.save(approval);

        ApplicationChoice choice = approval.getChoice();
        if (request.isApproved()) {
            choice.setStatus(ChoiceStatus.PENDING_COMMISSION);
        } else {
            choice.setStatus(ChoiceStatus.REJECTED);
            choice.setDecisionReason(request.getComment());
            choice.setDecidedAt(LocalDateTime.now());
            choice.setDecidedBy(directorId);
        }
        choiceRepository.save(choice);

        log.info("Réponse directeur thèse : approvalId={}, approved={}",
                approvalId, request.isApproved());
        return mapper.toThesisApprovalResponse(approval);
    }

    @Override
    @Transactional
    public void processExpiredApprovals() {
        approvalRepository.findExpiredPendingApprovals(LocalDateTime.now()).forEach(approval -> {
            approval.setStatus(ApprovalStatus.EXPIRED);
            approvalRepository.save(approval);

            ApplicationChoice choice = approval.getChoice();
            choice.setStatus(ChoiceStatus.REJECTED);
            choice.setDecisionReason("Délai d'accord du directeur de thèse expiré");
            choice.setDecidedAt(LocalDateTime.now());
            choiceRepository.save(choice);

            log.info("Accord directeur expiré : approvalId={}", approval.getId());
        });
    }

    @Override
    @Transactional
    public void sendReminders() {
        LocalDateTime reminderThreshold = LocalDateTime.now().minusDays(7);
        approvalRepository.findForReminder(reminderThreshold, LocalDateTime.now())
                .forEach(approval ->
                                log.info("Rappel directeur thèse : approvalId={}, directorId={}",
                                        approval.getId(), approval.getDirectorId())
                        // TODO : publier un event de rappel quand Notification Service sera implémenté
                );
    }
}