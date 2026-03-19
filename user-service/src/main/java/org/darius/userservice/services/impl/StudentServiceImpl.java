package org.darius.userservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.common.enums.FiliereStatus;
import org.darius.userservice.common.dtos.requests.TransferStudentRequest;
import org.darius.userservice.common.dtos.requests.UpdateStudentStatusRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.StudentAcademicHistoryResponse;
import org.darius.userservice.common.dtos.responses.StudentResponse;
import org.darius.userservice.common.dtos.responses.StudentSummaryResponse;
import org.darius.userservice.entities.*;
import org.darius.userservice.common.enums.HistoryChangeReason;
import org.darius.userservice.common.enums.StudentStatus;
import org.darius.userservice.events.consumes.ApplicationAcceptedEvent;
import org.darius.userservice.events.produces.StudentGraduatedEvent;
import org.darius.userservice.events.produces.StudentPromotedEvent;
import org.darius.userservice.events.produces.StudentTransferredEvent;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.kafka.UserEventProducer;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.repositories.*;
import org.darius.userservice.services.StudentService;
import org.darius.userservice.services.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository               studentRepository;
    private final StudentAcademicHistoryRepository historyRepository;
    private final UserProfileRepository           profileRepository;
    private final FiliereRepository               filiereRepository;
    private final StudyLevelRepository            studyLevelRepository;
    private final UserProfileService              userProfileService;
    private final UserEventProducer eventProducer;
    private final UserMapper userMapper;

    public StudentServiceImpl(StudentRepository studentRepository, StudentAcademicHistoryRepository historyRepository, UserProfileRepository profileRepository, FiliereRepository filiereRepository, StudyLevelRepository studyLevelRepository, UserProfileService userProfileService, UserEventProducer eventProducer, UserMapper userMapper) {
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.profileRepository = profileRepository;
        this.filiereRepository = filiereRepository;
        this.studyLevelRepository = studyLevelRepository;
        this.userProfileService = userProfileService;
        this.eventProducer = eventProducer;
        this.userMapper = userMapper;
    }

    // ── Création depuis Kafka ─────────────────────────────────────────────────

    @Override
    @Transactional
    public void createStudentFromAdmission(ApplicationAcceptedEvent event) {
        log.info("Création du profil étudiant depuis admission : userId={}", event.getUserId());

        // 1. Idempotence — vérifie si le profil existe déjà
        if (profileRepository.existsByUserId(event.getUserId())) {
            log.warn("Profil déjà existant pour userId={} — ignoré", event.getUserId());
            return;
        }

        // 2. Créer le UserProfile complet depuis CandidateProfile
        userProfileService.createFullProfileFromAdmission(
                event.getUserId(),
                event.getFirstName(), event.getLastName(),
                event.getPhone(), event.getNationality(),
                event.getGender(), event.getBirthPlace(),
                event.getBirthDate(),
                event.getPhotoUrl(), event.getPersonalEmail()
        );

        // 3. Récupérer le profil créé
        UserProfile profile = profileRepository.findByUserId(event.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Profil introuvable après création pour userId=" + event.getUserId()
                ));

        // 4. Récupérer la filière et le premier niveau
        Filiere filiere = filiereRepository.findById(event.getFiliereId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Filière introuvable : id=" + event.getFiliereId()
                ));

        StudyLevel firstLevel = studyLevelRepository
                .findByFiliere_IdAndOrder(event.getFiliereId(), 1)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun niveau trouvé pour filiereId=" + event.getFiliereId()
                ));

        // 5. Créer le dossier étudiant
        Student student = Student.builder()
                .profileId(profile.getId())
                .studentNumber(event.getStudentNumber())
                .enrollmentYear(Year.now().getValue())
                .filiere(filiere)
                .currentLevel(firstLevel)
                .status(StudentStatus.ACTIVE)
                .admissionApplicationId(event.getApplicationId())
                .build();

        student = studentRepository.save(student);

        // 6. Créer l'enregistrement initial dans l'historique académique
        createHistoryEntry(
                student,
                filiere.getId(), filiere.getName(),
                firstLevel.getId(), firstLevel.getLabel(),
                HistoryChangeReason.PROMOTION
        );

        // 7. Publier StudentProfileCreated
        eventProducer.publishStudentProfileCreated(student, profile, firstLevel);

        log.info("Profil étudiant créé : studentId={}, matricule={}",
                student.getId(), student.getStudentNumber());
    }

    // ── Consultation ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(String studentId) {
        Student student = findStudentOrThrow(studentId);
        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentByNumber(String studentNumber) {
        Student student = studentRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new UserNotFoundException(
                        "Étudiant introuvable : matricule=" + studentNumber
                ));
        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getMyStudentProfile(String userEmail) {
        UserProfile profile = profileRepository.findByPersonalEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable pour email=" + userEmail
                ));
        Student student = studentRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Dossier étudiant introuvable pour profileId=" + profile.getId()
                ));
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentSummaryResponse> getStudents(
            Long filiereId, Long levelId, StudentStatus status, int page, int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Student> studentPage = studentRepository.findWithFilters(
                filiereId, levelId, status, pageable
        );

        List<StudentSummaryResponse> content = studentPage.getContent().stream()
                .map(s -> {
                    UserProfile p = profileRepository.findById(s.getProfileId()).orElse(null);
                    return userMapper.toStudentSummaryResponse(s, p);
                })
                .toList();

        return PageResponse.<StudentSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(studentPage.getTotalElements())
                .totalPages(studentPage.getTotalPages())
                .last(studentPage.isLast())
                .build();
    }

    // ── Opérations académiques ────────────────────────────────────────────────

    @Override
    @Transactional
    public StudentResponse promoteStudent(String studentId) {
        Student student = findStudentOrThrow(studentId);
        validateActive(student, "promu");

        if (studyLevelRepository.isLastLevel(student.getCurrentLevel().getId())) {
            throw new InvalidOperationException(
                    "L'étudiant est déjà au dernier niveau de sa filière"
            );
        }

        StudyLevel nextLevel = studyLevelRepository
                .findNextLevel(student.getCurrentLevel().getId())
                .orElseThrow(() -> new IllegalStateException("Niveau suivant introuvable"));

        Long fromLevelId = student.getCurrentLevel().getId();
        String fromLevelLabel = student.getCurrentLevel().getLabel();

        // Fermer l'enregistrement courant dans l'historique
        closeCurrentHistoryEntry(student.getId());

        // Créer le nouvel enregistrement
        createHistoryEntry(
                student,
                student.getFiliere().getId(), student.getFiliere().getName(),
                nextLevel.getId(), nextLevel.getLabel(),
                HistoryChangeReason.PROMOTION
        );

        // Mettre à jour le niveau courant
        student.setCurrentLevel(nextLevel);
        student = studentRepository.save(student);

        // Publier l'événement
        eventProducer.publishStudentPromoted(StudentPromotedEvent.builder()
                .studentId(student.getId())
                .userId(findProfileByProfileIdOrThrow(student.getProfileId()).getUserId())
                .filiereId(student.getFiliere().getId())
                .fromLevelId(fromLevelId)
                .toLevelId(nextLevel.getId())
                .fromLevelLabel(fromLevelLabel)
                .toLevelLabel(nextLevel.getLabel())
                .academicYear(String.valueOf(Year.now().getValue()))
                .build()
        );

        log.info("Étudiant promu : studentId={}, {} → {}",
                studentId, fromLevelLabel, nextLevel.getLabel());

        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional
    public StudentResponse graduateStudent(String studentId) {
        Student student = findStudentOrThrow(studentId);
        validateActive(student, "diplômé");

        if (!studyLevelRepository.isLastLevel(student.getCurrentLevel().getId())) {
            throw new InvalidOperationException(
                    "L'étudiant n'est pas au dernier niveau de sa filière"
            );
        }

        // Fermer l'historique
        closeCurrentHistoryEntry(student.getId());

        student.setStatus(StudentStatus.GRADUATED);
        student = studentRepository.save(student);

        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());

        // Publier l'événement
        eventProducer.publishStudentGraduated(StudentGraduatedEvent.builder()
                .studentId(student.getId())
                .userId(profile.getUserId())
                .filiereId(student.getFiliere().getId())
                .filiereName(student.getFiliere().getName())
                .academicYear(String.valueOf(Year.now().getValue()))
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .build()
        );

        log.info("Étudiant diplômé : studentId={}", studentId);
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional
    public StudentResponse transferStudent(String studentId, TransferStudentRequest request) {
        Student student = findStudentOrThrow(studentId);
        validateActive(student, "transféré");

        Filiere targetFiliere = filiereRepository.findById(request.getTargetFiliereId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Filière cible introuvable : id=" + request.getTargetFiliereId()
                ));

        if (targetFiliere.getStatus() != FiliereStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "La filière cible n'est pas active"
            );
        }

        StudyLevel targetLevel = studyLevelRepository.findById(request.getTargetLevelId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Niveau cible introuvable : id=" + request.getTargetLevelId()
                ));

        Long fromFiliereId = student.getFiliere().getId();
        Long fromLevelId   = student.getCurrentLevel().getId();

        // Fermer l'historique courant
        closeCurrentHistoryEntry(student.getId());

        // Créer le nouvel enregistrement d'historique
        createHistoryEntry(
                student,
                targetFiliere.getId(), targetFiliere.getName(),
                targetLevel.getId(), targetLevel.getLabel(),
                request.getReason()
        );

        // Mettre à jour filière et niveau
        student.setFiliere(targetFiliere);
        student.setCurrentLevel(targetLevel);
        student = studentRepository.save(student);

        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());

        // Publier l'événement
        eventProducer.publishStudentTransferred(StudentTransferredEvent.builder()
                .studentId(student.getId())
                .userId(profile.getUserId())
                .fromFiliereId(fromFiliereId)
                .toFiliereId(targetFiliere.getId())
                .fromLevelId(fromLevelId)
                .toLevelId(targetLevel.getId())
                .reason(request.getReason().name())
                .build()
        );

        log.info("Étudiant transféré : studentId={}, filiere {} → {}",
                studentId, fromFiliereId, targetFiliere.getId());

        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional
    public StudentResponse updateStudentStatus(String studentId, UpdateStudentStatusRequest request) {
        Student student = findStudentOrThrow(studentId);

        log.info("Changement de statut étudiant : id={}, {} → {}",
                studentId, student.getStatus(), request.getStatus());

        student.setStatus(request.getStatus());
        student = studentRepository.save(student);

        UserProfile profile = findProfileByProfileIdOrThrow(student.getProfileId());
        return userMapper.toStudentResponse(student, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAcademicHistoryResponse> getAcademicHistory(String studentId) {
        findStudentOrThrow(studentId); // vérifie l'existence
        return userMapper.toHistoryResponseList(
                historyRepository.findByStudent_IdOrderByStartDateDesc(studentId)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Student findStudentOrThrow(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Étudiant introuvable : id=" + studentId
                ));
    }

    private UserProfile findProfileByProfileIdOrThrow(String profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable : id=" + profileId
                ));
    }

    private void validateActive(Student student, String action) {
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "L'étudiant ne peut pas être " + action +
                            " — statut actuel : " + student.getStatus()
            );
        }
    }

    private void closeCurrentHistoryEntry(String studentId) {
        historyRepository.findByStudent_IdAndEndDateIsNull(studentId)
                .ifPresent(entry -> {
                    entry.setEndDate(LocalDate.now());
                    historyRepository.save(entry);
                });
    }

    private void createHistoryEntry(
            Student student,
            Long filiereId, String filiereName,
            Long levelId, String levelLabel,
            HistoryChangeReason reason
    ) {
        StudentAcademicHistory history = StudentAcademicHistory.builder()
                .student(student)
                .filiereId(filiereId)
                .filiereName(filiereName)
                .levelId(levelId)
                .levelLabel(levelLabel)
                .academicYear(Year.now().getValue() + "-" + (Year.now().getValue() + 1))
                .startDate(LocalDate.now())
                .reason(reason)
                .build();

        historyRepository.save(history);
    }
}