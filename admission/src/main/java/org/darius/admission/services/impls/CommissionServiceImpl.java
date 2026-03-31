package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.AddCommissionMemberRequest;
import org.darius.admission.common.dtos.requests.CastVoteRequest;
import org.darius.admission.common.dtos.requests.ValidateDecisionRequest;
import org.darius.admission.common.dtos.responses.ChoiceResponse;
import org.darius.admission.common.dtos.responses.CommissionResponse;
import org.darius.admission.common.dtos.responses.VoteResponse;
import org.darius.admission.common.dtos.responses.VoteResultResponse;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.common.enums.MemberRole;
import org.darius.admission.common.enums.VoteType;
import org.darius.admission.common.enums.WaitlistStatus;
import org.darius.admission.entities.*;
import org.darius.admission.exceptions.DuplicateResourceException;
import org.darius.admission.exceptions.ForbiddenException;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.*;
import org.darius.admission.services.ApplicationEvaluationService;
import org.darius.admission.services.CommissionService;
import org.darius.admission.services.WaitlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements CommissionService {

    private final ReviewCommissionRepository commissionRepository;
    private final CommissionMemberRepository memberRepository;
    private final CommissionVoteRepository voteRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final WaitlistEntryRepository waitlistRepository;
    private final ApplicationEvaluationService evaluationService;
    private final WaitlistService waitlistService;
    private final AdmissionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommissionResponse> getAllCommissions() {
        return commissionRepository.findAll().stream()
                .map(mapper::toCommissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CommissionResponse getCommissionById(Long commissionId) {
        return mapper.toCommissionResponse(findCommissionOrThrow(commissionId));
    }

    @Override
    @Transactional
    public CommissionResponse addMember(Long commissionId, AddCommissionMemberRequest request) {
        ReviewCommission commission = findCommissionOrThrow(commissionId);

        if (memberRepository.existsByCommission_IdAndTeacherId(
                commissionId, request.getTeacherId())
        ) {
            throw new DuplicateResourceException("Cet enseignant est déjà membre de la commission");
        }

        // Si PRESIDENT → vérifier qu'il n'y en a pas déjà un
        if (request.getRole() == MemberRole.PRESIDENT) {
            memberRepository.findByCommission_IdAndRole(commissionId, MemberRole.PRESIDENT)
                    .ifPresent(existing -> {
                        throw new InvalidOperationException(
                                "La commission a déjà un président"
                        );
                    });
            commission.setPresidentId(request.getTeacherId());
            commissionRepository.save(commission);
        }

        CommissionMember member = CommissionMember.builder()
                .commission(commission)
                .teacherId(request.getTeacherId())
                .role(request.getRole())
                .build();

        memberRepository.save(member);
        return mapper.toCommissionResponse(commissionRepository.findById(commissionId).orElseThrow());
    }

    @Override
    @Transactional
    public CommissionResponse removeMember(Long commissionId, Long memberId) {
        CommissionMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : id=" + memberId));

        if (member.getRole() == MemberRole.PRESIDENT) {
            ReviewCommission commission = findCommissionOrThrow(commissionId);
            commission.setPresidentId(null);
            commissionRepository.save(commission);
        }

        memberRepository.delete(member);
        return mapper.toCommissionResponse(findCommissionOrThrow(commissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoiceResponse> getPendingChoicesForCommission(
            Long commissionId, String teacherId
    ) {
        if (!memberRepository.existsByCommission_IdAndTeacherId(commissionId, teacherId)) {
            throw new ForbiddenException("Vous n'êtes pas membre de cette commission");
        }

        ReviewCommission commission = findCommissionOrThrow(commissionId);
        return choiceRepository.findPendingChoicesByOffer(commission.getOffer().getId())
                .stream()
                .map(mapper::toChoiceResponse)
                .toList();
    }

    @Override
    @Transactional
    public VoteResponse castVote(Long commissionId, String teacherId, CastVoteRequest request) {
        ReviewCommission commission = findCommissionOrThrow(commissionId);

        // Vérifier que le votant est membre
        CommissionMember member = memberRepository
                .findByCommission_IdAndTeacherId(commissionId, teacherId)
                .orElseThrow(() -> new ForbiddenException("Vous n'êtes pas membre de cette commission"));

        // Vérifier que le choix appartient à la commission
        ApplicationChoice choice = choiceRepository.findById(request.getChoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Choix introuvable : id=" + request.getChoiceId()
                ));

        if (choice.getStatus() != ChoiceStatus.UNDER_COMMISSION_REVIEW
                && choice.getStatus() != ChoiceStatus.PENDING_COMMISSION) {
            throw new InvalidOperationException(
                    "Ce choix n'est pas en cours d'examen par la commission"
            );
        }

        // Vérifier qu'il n'a pas déjà voté
        if (voteRepository.existsByChoice_IdAndMemberId(request.getChoiceId(), teacherId)) {
            throw new DuplicateResourceException("Vous avez déjà voté pour ce dossier");
        }

        // Passer le choix en UNDER_COMMISSION_REVIEW si ce n'est pas encore fait
        if (choice.getStatus() == ChoiceStatus.PENDING_COMMISSION) {
            choice.setStatus(ChoiceStatus.UNDER_COMMISSION_REVIEW);
            choiceRepository.save(choice);
        }

        CommissionVote vote = CommissionVote.builder()
                .choice(choice)
                .commission(commission)
                .memberId(teacherId)
                .vote(request.getVote())
                .comment(request.getComment())
                .build();

        vote = voteRepository.save(vote);
        log.info("Vote enregistré : choiceId={}, membre={}, vote={}",
                request.getChoiceId(), teacherId, request.getVote());

        return mapper.toVoteResponse(vote);
    }

    @Override
    @Transactional(readOnly = true)
    public VoteResultResponse getVoteResult(Long commissionId, Long choiceId) {
        ReviewCommission commission = findCommissionOrThrow(commissionId);

        long accept   = voteRepository.countByChoice_IdAndVote(choiceId, VoteType.ACCEPT);
        long reject   = voteRepository.countByChoice_IdAndVote(choiceId, VoteType.REJECT);
        long abstain  = voteRepository.countByChoice_IdAndVote(choiceId, VoteType.ABSTAIN);
        long total    = accept + reject + abstain;
        boolean quorumReached = total >= commission.getQuorum();

        String suggested;
        if (!quorumReached) {
            suggested = "QUORUM_NOT_REACHED";
        } else if (accept > reject) {
            suggested = "ACCEPTED";
        } else if (reject > accept) {
            suggested = "REJECTED";
        } else {
            suggested = "TIE"; // voix du président prépondérante
        }

        return VoteResultResponse.builder()
                .choiceId(choiceId)
                .totalVotes((int) total)
                .acceptVotes((int) accept)
                .rejectVotes((int) reject)
                .abstainVotes((int) abstain)
                .quorumReached(quorumReached)
                .suggestedDecision(suggested)
                .build();
    }

    @Override
    @Transactional
    public ChoiceResponse validateDecision(
            Long commissionId,
            Long choiceId,
            String presidentId,
            ValidateDecisionRequest request
    ) {
        ReviewCommission commission = findCommissionOrThrow(commissionId);

        // Vérifier que c'est le président
        if (!presidentId.equals(commission.getPresidentId())) {
            throw new ForbiddenException("Seul le président peut valider la décision finale");
        }

        ApplicationChoice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable"));

        ChoiceStatus decision = request.getDecision();

        // Valider les décisions possibles
        if (!List.of(
                ChoiceStatus.ACCEPTED,
                ChoiceStatus.REJECTED,
                ChoiceStatus.WAITLISTED,
                ChoiceStatus.INTERVIEW_REQUIRED
        ).contains(decision)) {
            throw new InvalidOperationException(
                    "Décision invalide. Valeurs acceptées : ACCEPTED, REJECTED, WAITLISTED, INTERVIEW_REQUIRED"
            );
        }

        choice.setStatus(decision);
        choice.setDecidedAt(LocalDateTime.now());
        choice.setDecidedBy(presidentId);
        if (request.getReason() != null) {
            choice.setDecisionReason(request.getReason());
        }

        // Si WAITLISTED → créer WaitlistEntry
        if (decision == ChoiceStatus.WAITLISTED) {
            int rank = waitlistRepository.getNextRank(choice.getOffer().getId());
            WaitlistEntry entry = WaitlistEntry.builder()
                    .choice(choice)
                    .offerId(choice.getOffer().getId())
                    .rank(rank)
                    .status(WaitlistStatus.WAITING)
                    .build();
            waitlistRepository.save(entry);
            choice.getOffer().setWaitlistCount(choice.getOffer().getWaitlistCount() + 1);
        }

        choice = choiceRepository.save(choice);

        // Si décision finale → évaluation globale de la candidature
        if (decision == ChoiceStatus.ACCEPTED
                || decision == ChoiceStatus.REJECTED
                || decision == ChoiceStatus.WAITLISTED) {
            evaluationService.evaluateApplication(choice.getApplication().getId());
        }

        log.info("Décision validée : choiceId={}, decision={}", choiceId, decision);
        return mapper.toChoiceResponse(choice);
    }

    private ReviewCommission findCommissionOrThrow(Long id) {
        return commissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commission introuvable : id=" + id));
    }
}