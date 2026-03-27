package org.darius.admission.services.impls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.admission.common.dtos.requests.CompleteInterviewRequest;
import org.darius.admission.common.dtos.requests.ScheduleInterviewRequest;
import org.darius.admission.common.dtos.responses.InterviewResponse;
import org.darius.admission.common.enums.ChoiceStatus;
import org.darius.admission.common.enums.InterviewStatus;
import org.darius.admission.entities.ApplicationChoice;
import org.darius.admission.entities.Interview;
import org.darius.admission.evens.published.InterviewScheduledEvent;
import org.darius.admission.exceptions.InvalidOperationException;
import org.darius.admission.exceptions.ResourceNotFoundException;
import org.darius.admission.kafka.AdmissionEventProducer;
import org.darius.admission.mappers.AdmissionMapper;
import org.darius.admission.repositories.ApplicationChoiceRepository;
import org.darius.admission.repositories.InterviewRepository;
import org.darius.admission.services.InterviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationChoiceRepository choiceRepository;
    private final AdmissionEventProducer eventProducer;
    private final AdmissionMapper mapper;

    @Override
    @Transactional
    public InterviewResponse scheduleInterview(
            String applicationId, Long choiceId, ScheduleInterviewRequest request
    ) {
        ApplicationChoice choice = choiceRepository.findById(choiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Choix introuvable : id=" + choiceId));

        if (choice.getStatus() != ChoiceStatus.INTERVIEW_REQUIRED) {
            throw new InvalidOperationException(
                    "Un entretien ne peut être planifié que si le statut est INTERVIEW_REQUIRED"
            );
        }

        Interview interview = Interview.builder()
                .application(choice.getApplication())
                .choice(choice)
                .scheduledAt(request.getScheduledAt())
                .duration(request.getDuration())
                .location(request.getLocation())
                .type(request.getType())
                .status(InterviewStatus.SCHEDULED)
                .interviewers(request.getInterviewerIds())
                .build();

        interview = interviewRepository.save(interview);

        choice.setStatus(ChoiceStatus.INTERVIEW_SCHEDULED);
        choiceRepository.save(choice);

        eventProducer.publishInterviewScheduled(
                InterviewScheduledEvent.builder()
                        .applicationId(applicationId)
                        .userId(choice.getApplication().getUserId())
                        .interviewId(interview.getId())
                        .choiceId(choiceId)
                        .filiereName(choice.getFiliereName())
                        .scheduledAt(request.getScheduledAt())
                        .duration(request.getDuration())
                        .location(request.getLocation())
                        .type(request.getType().name())
                        .build()
        );

        log.info("Entretien planifié : choiceId={}, scheduledAt={}", choiceId, request.getScheduledAt());
        return mapper.toInterviewResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long interviewId) {
        return mapper.toInterviewResponse(findOrThrow(interviewId));
    }

    @Override
    @Transactional
    public InterviewResponse completeInterview(Long interviewId, CompleteInterviewRequest request) {
        Interview interview = findOrThrow(interviewId);

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new InvalidOperationException("L'entretien n'est pas en statut SCHEDULED");
        }

        interview.setStatus(InterviewStatus.DONE);
        interview.setNotes(request.getNotes()); // confidentiel

        // Passer le choix en INTERVIEW_DONE
        ApplicationChoice choice = interview.getChoice();
        choice.setStatus(ChoiceStatus.INTERVIEW_DONE);
        choiceRepository.save(choice);

        interview = interviewRepository.save(interview);
        log.info("Entretien clôturé : interviewId={}", interviewId);
        return mapper.toInterviewResponse(interview);
    }

    @Override
    @Transactional
    public InterviewResponse cancelInterview(Long interviewId, String reason) {
        Interview interview = findOrThrow(interviewId);

        if (interview.getStatus() != InterviewStatus.SCHEDULED) {
            throw new InvalidOperationException("L'entretien n'est pas en statut SCHEDULED");
        }

        // Vérification délai minimum 48h
        long hoursUntil = ChronoUnit.HOURS.between(LocalDateTime.now(), interview.getScheduledAt());
        if (hoursUntil < 48) {
            throw new InvalidOperationException(
                    "L'annulation doit être effectuée au moins 48h avant l'entretien"
            );
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        interview.setNotes(reason);

        // Repasser le choix en INTERVIEW_REQUIRED pour replanification
        ApplicationChoice choice = interview.getChoice();
        choice.setStatus(ChoiceStatus.INTERVIEW_REQUIRED);
        choiceRepository.save(choice);

        return mapper.toInterviewResponse(interviewRepository.save(interview));
    }

    private Interview findOrThrow(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entretien introuvable : id=" + id));
    }
}