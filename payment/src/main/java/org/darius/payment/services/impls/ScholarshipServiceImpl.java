package org.darius.payment.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.payment.common.dtos.requests.*;
import org.darius.payment.common.dtos.responses.*;
import org.darius.payment.entities.*;
import org.darius.payment.common.enums.*;
import org.darius.payment.events.consumed.SemesterValidatedEvent;
import org.darius.payment.events.published.*;
import org.darius.payment.exceptions.*;
import org.darius.payment.kafka.PaymentEventProducer;
import org.darius.payment.mappers.PaymentMapper;
import org.darius.payment.repositories.*;
import org.darius.payment.services.PaymentReferenceService;
import org.darius.payment.services.ScholarshipService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScholarshipServiceImpl implements ScholarshipService {

    private final ScholarshipRepository             scholarshipRepository;
    private final ScholarshipDisbursementRepository disbursementRepository;
    private final PaymentRepository                 paymentRepository;
    private final PaymentReferenceService           referenceService;
    private final PaymentEventProducer              eventProducer;
    private final PaymentMapper                     mapper;
    private final RestClient                        restClient;

    @Override
    @Transactional
    public ScholarshipResponse createScholarship(String adminId, CreateScholarshipRequest request) {
        if (scholarshipRepository.existsByStudentIdAndAcademicYearAndType(
                request.getStudentId(), request.getAcademicYear(), request.getType()
        )) {
            throw new DuplicateResourceException(
                    "Une bourse " + request.getType() + " existe déjà pour cet étudiant sur " + request.getAcademicYear()
            );
        }

        BigDecimal disbursementAmount = calculateDisbursementAmount(
                request.getAmount(), request.getDisbursementFrequency()
        );

        Scholarship scholarship = Scholarship.builder()
                .studentId(request.getStudentId())
                .academicYear(request.getAcademicYear())
                .amount(request.getAmount())
                .disbursementAmount(disbursementAmount)
                .disbursementFrequency(request.getDisbursementFrequency())
                .type(request.getType())
                .source(request.getSource())
                .status(ScholarshipStatus.PENDING)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .conditions(request.getConditions())
                .minimumGrade(request.getMinimumGrade() != null
                        ? request.getMinimumGrade() : new BigDecimal("14.0"))
                .createdBy(adminId)
                .build();

        return mapper.toScholarshipResponse(scholarshipRepository.save(scholarship));
    }

    @Override
    @Transactional
    public ScholarshipResponse activateScholarship(Long scholarshipId, String adminId) {
        Scholarship scholarship = findOrThrow(scholarshipId);

        if (scholarship.getStatus() != ScholarshipStatus.PENDING
                && scholarship.getStatus() != ScholarshipStatus.SUSPENDED) {
            throw new InvalidOperationException(
                    "Seule une bourse PENDING ou SUSPENDED peut être activée"
            );
        }

        scholarship.setStatus(ScholarshipStatus.ACTIVE);
        scholarshipRepository.save(scholarship);

        // Générer les versements UNIQUEMENT pour les bourses institutionnelles
        if (scholarship.getSource() == ScholarshipSource.INSTITUTIONNELLE) {
            generateDisbursements(scholarship);
        }

        eventProducer.publishScholarshipActivated(
                ScholarshipActivatedEvent.builder()
                        .scholarshipId(scholarship.getId())
                        .studentId(scholarship.getStudentId())
                        .type(scholarship.getType().name())
                        .amount(scholarship.getAmount())
                        .disbursementAmount(scholarship.getDisbursementAmount())
                        .frequency(scholarship.getDisbursementFrequency().name())
                        .academicYear(scholarship.getAcademicYear())
                        .build()
        );

        return mapper.toScholarshipResponse(scholarship);
    }

    @Override
    @Transactional
    public ScholarshipResponse suspendScholarship(
            Long scholarshipId, String adminId, SuspendScholarshipRequest request
    ) {
        Scholarship scholarship = findOrThrow(scholarshipId);

        if (scholarship.getStatus() != ScholarshipStatus.ACTIVE) {
            throw new InvalidOperationException("Seule une bourse ACTIVE peut être suspendue");
        }

        scholarship.setStatus(ScholarshipStatus.SUSPENDED);
        scholarship.setSuspensionReason(request.getReason());

        // Annuler les versements futurs
        disbursementRepository.findByScholarship_IdAndStatus(
                scholarshipId, DisbursementStatus.SCHEDULED
        ).forEach(d -> {
            d.setStatus(DisbursementStatus.CANCELLED);
            disbursementRepository.save(d);
        });

        scholarshipRepository.save(scholarship);

        eventProducer.publishScholarshipSuspended(
                ScholarshipSuspendedEvent.builder()
                        .scholarshipId(scholarshipId)
                        .studentId(scholarship.getStudentId())
                        .reason(request.getReason())
                        .build()
        );

        return mapper.toScholarshipResponse(scholarship);
    }

    @Override
    @Transactional
    public ScholarshipResponse terminateScholarship(Long scholarshipId, String adminId) {
        Scholarship scholarship = findOrThrow(scholarshipId);

        scholarship.setStatus(ScholarshipStatus.TERMINATED);

        disbursementRepository.findByScholarship_IdAndStatus(
                scholarshipId, DisbursementStatus.SCHEDULED
        ).forEach(d -> {
            d.setStatus(DisbursementStatus.CANCELLED);
            disbursementRepository.save(d);
        });

        return mapper.toScholarshipResponse(scholarshipRepository.save(scholarship));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getMyScholarships(String studentId) {
        return scholarshipRepository
                .findByStudentIdAndStatus(studentId, ScholarshipStatus.ACTIVE)
                .stream()
                .map(mapper::toScholarshipResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ScholarshipResponse getScholarshipById(Long scholarshipId) {
        return mapper.toScholarshipResponse(findOrThrow(scholarshipId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScholarshipResponse> getScholarships(String studentId, String type, String status) {
        return scholarshipRepository.findAll().stream()
                .filter(s -> studentId == null || s.getStudentId().equals(studentId))
                .filter(s -> type   == null || s.getType().name().equals(type))
                .filter(s -> status == null || s.getStatus().name().equals(status))
                .map(mapper::toScholarshipResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisbursementResponse> getDisbursements(Long scholarshipId) {
        return disbursementRepository
                .findByScholarship_IdOrderByScheduledDateDesc(scholarshipId)
                .stream()
                .map(mapper::toDisbursementResponse)
                .toList();
    }

    @Override
    @Transactional
    public void processDisbursements() {
        disbursementRepository.findDueForProcessing(LocalDate.now()).forEach(disbursement -> {
            // Ne verser QUE les bourses institutionnelles
            if (disbursement.getScholarship().getSource() != ScholarshipSource.INSTITUTIONNELLE) {
                log.debug("Versement ignoré - bourse non institutionnelle : scholarshipId={}",
                        disbursement.getScholarship().getId());
                disbursement.setStatus(DisbursementStatus.CANCELLED);
                disbursementRepository.save(disbursement);
                return;
            }

            try {
                Payment payment = Payment.builder()
                        .userId(disbursement.getScholarship().getStudentId())
                        .amount(disbursement.getAmount())
                        .currency("EUR")
                        .type(PaymentType.BOURSE)
                        .status(PaymentStatus.COMPLETED)
                        .method(PaymentMethod.VIREMENT)
                        .reference(referenceService.generateReference())
                        .description("Versement bourse " + disbursement.getPeriod())
                        .paidAt(LocalDateTime.now())
                        .build();

                payment = paymentRepository.save(payment);

                disbursement.setStatus(DisbursementStatus.PAID);
                disbursement.setPaymentId(payment.getId());
                disbursement.setPaidAt(LocalDateTime.now());
                disbursementRepository.save(disbursement);

                eventProducer.publishScholarshipDisbursed(
                        ScholarshipDisbursedEvent.builder()
                                .scholarshipId(disbursement.getScholarship().getId())
                                .studentId(disbursement.getScholarship().getStudentId())
                                .amount(disbursement.getAmount())
                                .period(disbursement.getPeriod())
                                .paymentReference(payment.getReference())
                                .build()
                );

                log.info("Versement bourse traité : studentId={}, période={}, montant={}",
                        disbursement.getScholarship().getStudentId(),
                        disbursement.getPeriod(),
                        disbursement.getAmount());

            } catch (Exception ex) {
                disbursement.setStatus(DisbursementStatus.FAILED);
                disbursement.setFailureReason(ex.getMessage());
                disbursementRepository.save(disbursement);
                log.error("Échec versement bourse : scholarshipId={} — {}",
                        disbursement.getScholarship().getId(), ex.getMessage());
            }
        });
    }

    @Override
    @Transactional
    public void processAnnualMeritRenewal(String academicYear) {
        scholarshipRepository.findByTypeAndStatus(
                ScholarshipType.MERITE, ScholarshipStatus.ACTIVE
        ).forEach(scholarship -> checkAndRenewMerit(scholarship, academicYear));
    }

    @Override
    @Transactional
    public void processMeritRenewalFromEvent(SemesterValidatedEvent event) {
        event.getResults().forEach(result -> {
            Optional<Scholarship> opt = scholarshipRepository.findByStudentIdAndAcademicYearAndStatus(
                    result.getStudentId(), event.getAcademicYear(), ScholarshipStatus.ACTIVE
            );

            opt.filter(s -> s.getType() == ScholarshipType.MERITE).ifPresent(scholarship -> {
                boolean eligible = result.getSemesterAverage() != null
                        && result.getSemesterAverage() >= scholarship.getMinimumGrade().doubleValue();

                if (!eligible) {
                    scholarship.setStatus(ScholarshipStatus.SUSPENDED);
                    scholarship.setSuspensionReason(
                            "Moyenne insuffisante : " + result.getSemesterAverage() +
                                    " (requis : " + scholarship.getMinimumGrade() + ")"
                    );
                    scholarshipRepository.save(scholarship);
                    log.info("Bourse mérite suspendue : studentId={}, moyenne={}",
                            result.getStudentId(), result.getSemesterAverage());
                }
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkAndRenewMerit(Scholarship scholarship, String nextYear) {
        try {
            Map progress = restClient.get()
                    .uri("http://localhost:8083/courses/students/"
                            + scholarship.getStudentId() + "/progress?year=" + scholarship.getAcademicYear())
                    .retrieve()
                    .body(Map.class);

            if (progress == null) return;

            Double avg = (Double) progress.get("annualAverage");
            if (avg != null && avg >= scholarship.getMinimumGrade().doubleValue()) {
                // Renouveler pour l'année suivante
                CreateScholarshipRequest renewRequest = CreateScholarshipRequest.builder()
                        .studentId(scholarship.getStudentId())
                        .academicYear(nextYear)
                        .amount(scholarship.getAmount())
                        .disbursementFrequency(scholarship.getDisbursementFrequency())
                        .type(ScholarshipType.MERITE)
                        .startDate(LocalDate.of(Integer.parseInt(nextYear.split("-")[0]), 9, 1))
                        .endDate(LocalDate.of(Integer.parseInt(nextYear.split("-")[1]), 6, 30))
                        .minimumGrade(scholarship.getMinimumGrade())
                        .build();

                createScholarship("system", renewRequest);
                log.info("Bourse mérite renouvelée : studentId={}, année={}", scholarship.getStudentId(), nextYear);
            } else {
                scholarship.setStatus(ScholarshipStatus.SUSPENDED);
                scholarship.setSuspensionReason("Moyenne annuelle insuffisante : " + avg);
                scholarshipRepository.save(scholarship);
            }
        } catch (Exception ex) {
            log.warn("Impossible de vérifier la progression de {} : {}", scholarship.getStudentId(), ex.getMessage());
        }
    }

    private void generateDisbursements(Scholarship scholarship) {
        int nbPeriods = switch (scholarship.getDisbursementFrequency()) {
            case MONTHLY   -> 12;
            case QUARTERLY -> 4;
            case SEMESTER  -> 2;
        };

        LocalDate start = scholarship.getStartDate();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 0; i < nbPeriods; i++) {
            LocalDate scheduledDate = switch (scholarship.getDisbursementFrequency()) {
                case MONTHLY   -> start.plusMonths(i);
                case QUARTERLY -> start.plusMonths(i * 3L);
                case SEMESTER  -> start.plusMonths(i * 6L);
            };

            ScholarshipDisbursement disbursement = ScholarshipDisbursement.builder()
                    .scholarship(scholarship)
                    .amount(scholarship.getDisbursementAmount())
                    .period(scheduledDate.format(fmt))
                    .scheduledDate(scheduledDate)
                    .status(DisbursementStatus.SCHEDULED)
                    .build();

            disbursementRepository.save(disbursement);
        }
    }

    private BigDecimal calculateDisbursementAmount(BigDecimal annual, DisbursementFrequency freq) {
        int divisor = switch (freq) {
            case MONTHLY   -> 12;
            case QUARTERLY -> 4;
            case SEMESTER  -> 2;
        };
        return annual.divide(BigDecimal.valueOf(divisor), 2, java.math.RoundingMode.HALF_UP);
    }

    private Scholarship findOrThrow(Long id) {
        return scholarshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bourse introuvable : id=" + id));
    }
}
