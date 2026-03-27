package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateSemesterRequest;
import org.darius.course.dtos.responses.SemesterResponse;
import org.darius.course.dtos.responses.StudentProgressResponse;
import org.darius.course.entities.Semester;
import org.darius.course.enums.SemesterStatus;
import org.darius.course.events.published.SemesterValidatedEvent;
import org.darius.course.exceptions.*;
import org.darius.course.kafka.CourseEventProducer;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.SemesterRepository;
import org.darius.course.services.SemesterService;
import org.darius.course.services.StudentProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository       semesterRepository;
    private final StudentProgressService   progressService;
    private final CourseEventProducer      eventProducer;
    private final CourseMapper             mapper;

    @Override
    @Transactional
    public SemesterResponse create(CreateSemesterRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidOperationException(
                    "La date de fin doit être postérieure à la date de début"
            );
        }

        Semester semester = Semester.builder()
                .label(request.getLabel())
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isLastOfYear(request.isLastOfYear())
                .status(SemesterStatus.UPCOMING)
                .build();

        // Si le semestre commence aujourd'hui ou avant → ACTIVE directement
        if (!request.getStartDate().isAfter(LocalDate.now())) {
            semester.setStatus(SemesterStatus.ACTIVE);
            semester.setCurrent(true);
            // Désactiver l'ancien semestre courant
            semesterRepository.findByIsCurrent(true).ifPresent(old -> {
                old.setCurrent(false);
                semesterRepository.save(old);
            });
        }

        return mapper.toSemesterResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getById(Long id) {
        return mapper.toSemesterResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getCurrent() {
        return mapper.toSemesterResponse(
                semesterRepository.findByIsCurrent(true)
                        .orElseThrow(() -> new ResourceNotFoundException("Aucun semestre actif"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemesterResponse> getAll() {
        return semesterRepository.findAll().stream()
                .map(mapper::toSemesterResponse)
                .toList();
    }

    @Override
    @Transactional
    public SemesterResponse close(Long id) {
        Semester semester = findOrThrow(id);

        if (semester.getStatus() != SemesterStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Seul un semestre ACTIVE peut être clôturé"
            );
        }

        semester.setStatus(SemesterStatus.CLOSED);
        semester.setCurrent(false);
        log.info("Semestre {} clôturé", semester.getLabel());
        return mapper.toSemesterResponse(semesterRepository.save(semester));
    }

    @Override
    @Transactional
    public void computeProgress(Long semesterId) {
        Semester semester = findOrThrow(semesterId);

        if (semester.getStatus() != SemesterStatus.CLOSED) {
            throw new InvalidOperationException(
                    "Le semestre doit être CLOSED pour calculer les progressions"
            );
        }

        log.info("Calcul des progressions pour le semestre {}", semester.getLabel());
        progressService.computeAllForSemester(semesterId);
        log.info("Progressions calculées avec succès pour {}", semester.getLabel());
    }

    @Override
    @Transactional
    public SemesterResponse validate(Long semesterId) {
        Semester semester = findOrThrow(semesterId);

        if (semester.getStatus() != SemesterStatus.CLOSED) {
            throw new InvalidOperationException(
                    "Le semestre doit être CLOSED avant validation"
            );
        }

        if (!semesterRepository.existsById(semesterId)) {
            throw new ResourceNotFoundException("Semestre introuvable");
        }

        // Vérifier que tous les étudiants ont un StudentProgress
        // (délégué au repository via une query COUNT)
        List<StudentProgressResponse> allProgress =
                progressService.getBySemester(semesterId);

        if (allProgress.isEmpty()) {
            throw new InvalidOperationException(
                    "Aucune progression calculée — lancez d'abord compute-progress"
            );
        }

        semester.setStatus(SemesterStatus.VALIDATED);
        semesterRepository.save(semester);

        // Construire et publier SemesterValidatedEvent
        List<SemesterValidatedEvent.StudentResult> results = allProgress.stream()
                .map(p -> SemesterValidatedEvent.StudentResult.builder()
                        .studentId(p.getStudentId())
                        .semesterAverage(p.getSemesterAverage())
                        .creditsObtained(p.getCreditsObtained())
                        .status(p.getStatus().name())
                        .mention(p.getMention().name())
                        .isAdmis(p.isAdmis())
                        .rank(p.getRank())
                        .build()
                )
                .toList();

        SemesterValidatedEvent event = SemesterValidatedEvent.builder()
                .semesterId(semester.getId())
                .semesterLabel(semester.getLabel())
                .academicYear(semester.getAcademicYear())
                .isLastSemester(semester.isLastOfYear())
                .results(results)
                .build();

        eventProducer.publishSemesterValidated(event);

        log.info("Semestre {} VALIDÉ — {} étudiants, event publié",
                semester.getLabel(), results.size());

        return mapper.toSemesterResponse(semester);
    }

    @Override
    @Transactional
    public void updateSemesterStatuses() {
        LocalDate today = LocalDate.now();

        // UPCOMING → ACTIVE si startDate atteinte
        semesterRepository.findUpcomingToActivate(today).forEach(semester -> {
            semester.setStatus(SemesterStatus.ACTIVE);

            // Désactiver l'ancien semestre courant
            semesterRepository.findByIsCurrent(true).ifPresent(old -> {
                if (!old.getId().equals(semester.getId())) {
                    old.setCurrent(false);
                    semesterRepository.save(old);
                }
            });

            semester.setCurrent(true);
            semesterRepository.save(semester);
            log.info("Semestre {} activé automatiquement", semester.getLabel());
        });
    }

    private Semester findOrThrow(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable : id=" + id));
    }
}
