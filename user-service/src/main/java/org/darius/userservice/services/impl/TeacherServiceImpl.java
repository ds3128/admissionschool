package org.darius.userservice.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.common.dtos.requests.CreateTeacherRequest;
import org.darius.userservice.common.dtos.requests.UpdateTeacherRequest;
import org.darius.userservice.common.dtos.responses.PageResponse;
import org.darius.userservice.common.dtos.responses.TeacherResponse;
import org.darius.userservice.common.dtos.responses.TeacherSummaryResponse;
import org.darius.userservice.entities.Department;
import org.darius.userservice.entities.Teacher;
import org.darius.userservice.entities.UserProfile;
import org.darius.userservice.events.produces.TeacherCreationRequestedEvent;
import org.darius.userservice.events.produces.TeacherDeactivatedEvent;
import org.darius.userservice.exceptions.DuplicateResourceException;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.kafka.UserEventProducer;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.repositories.DepartmentRepository;
import org.darius.userservice.repositories.TeacherRepository;
import org.darius.userservice.repositories.UserProfileRepository;
import org.darius.userservice.services.TeacherService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository     teacherRepository;
    private final UserProfileRepository profileRepository;
    private final DepartmentRepository  departmentRepository;
    private final UserEventProducer eventProducer;
    private final UserMapper userMapper;

    public TeacherServiceImpl(TeacherRepository teacherRepository, UserProfileRepository profileRepository, DepartmentRepository departmentRepository, UserEventProducer eventProducer, UserMapper userMapper) {
        this.teacherRepository = teacherRepository;
        this.profileRepository = profileRepository;
        this.departmentRepository = departmentRepository;
        this.eventProducer = eventProducer;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request, String createdByUserId) {
        log.info("Création du profil enseignant : email={}", request.getPersonalEmail());

        // 1. Vérifie que l'email n'est pas déjà utilisé
        if (profileRepository.existsByPersonalEmail(request.getPersonalEmail())) {
            throw new DuplicateResourceException(
                    "L'email '" + request.getPersonalEmail() + "' est déjà utilisé"
            );
        }

        // 2. Vérifie que le département existe
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Département introuvable : id=" + request.getDepartmentId()
                ));

        // 3. Génère le numéro d'employé
        String employeeNumber = generateEmployeeNumber();

        // 4. Publie la demande de création de compte vers l'Auth Service
        // Le Auth Service répondra avec userId + institutionalEmail via un autre event
        // Pour l'instant on génère un requestId pour corréler la réponse
        String requestId = UUID.randomUUID().toString();

        eventProducer.publishTeacherCreationRequested(
                TeacherCreationRequestedEvent.builder()
                        .requestId(requestId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .personalEmail(request.getPersonalEmail())
                        .role("TEACHER")
                        .build()
        );

        // 5. Crée le UserProfile (userId sera mis à jour à la réception de la réponse Auth)
        UserProfile profile = UserProfile.builder()
                .userId(requestId)  // placeholder — remplacé par le vrai userId Auth
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .personalEmail(request.getPersonalEmail())
                .build();

        profile = profileRepository.save(profile);

        // 6. Crée le Teacher
        Teacher teacher = Teacher.builder()
                .profileId(profile.getId())
                .userId(requestId)  // placeholder
                .employeeNumber(employeeNumber)
                .speciality(request.getSpeciality())
                .grade(request.getGrade())
                .department(department)
                .diploma(request.getDiploma())
                .maxHoursPerWeek(request.getMaxHoursPerWeek())
                .active(true)
                .createdBy(createdByUserId)
                .build();

        teacher = teacherRepository.save(teacher);
        log.info("Profil enseignant créé : id={}, employeeNumber={}", teacher.getId(), employeeNumber);

        return userMapper.toTeacherResponse(teacher, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(String teacherId) {
        Teacher teacher = findTeacherOrThrow(teacherId);
        UserProfile profile = findProfileOrThrow(teacher.getProfileId());
        return userMapper.toTeacherResponse(teacher, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getMyTeacherProfile(String userEmail) {
        UserProfile profile = profileRepository.findByPersonalEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable pour email=" + userEmail
                ));
        Teacher teacher = teacherRepository.findByProfileId(profile.getId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Dossier enseignant introuvable pour profileId=" + profile.getId()
                ));
        return userMapper.toTeacherResponse(teacher, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherSummaryResponse> getTeachers(
            Long departmentId, Boolean isActive, int page, int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Teacher> teacherPage = teacherRepository.findWithFilters(departmentId, isActive, pageable);

        List<TeacherSummaryResponse> content = teacherPage.getContent().stream()
                .map(t -> {
                    UserProfile p = profileRepository.findById(t.getProfileId()).orElse(null);
                    return userMapper.toTeacherSummaryResponse(t, p);
                })
                .toList();

        return PageResponse.<TeacherSummaryResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(teacherPage.getTotalElements())
                .totalPages(teacherPage.getTotalPages())
                .last(teacherPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(String teacherId, UpdateTeacherRequest request) {
        Teacher teacher = findTeacherOrThrow(teacherId);

        if (request.getSpeciality()      != null) teacher.setSpeciality(request.getSpeciality());
        if (request.getGrade()           != null) teacher.setGrade(request.getGrade());
        if (request.getDiploma()         != null) teacher.setDiploma(request.getDiploma());
        if (request.getMaxHoursPerWeek() != null) teacher.setMaxHoursPerWeek(request.getMaxHoursPerWeek());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new UserNotFoundException(
                            "Département introuvable : id=" + request.getDepartmentId()
                    ));
            teacher.setDepartment(department);
        }

        teacher = teacherRepository.save(teacher);
        UserProfile profile = findProfileOrThrow(teacher.getProfileId());
        return userMapper.toTeacherResponse(teacher, profile);
    }

    @Override
    @Transactional
    public TeacherResponse deactivateTeacher(String teacherId) {
        Teacher teacher = findTeacherOrThrow(teacherId);

        if (!teacher.isActive()) {
            throw new InvalidOperationException("L'enseignant est déjà inactif");
        }

        teacher.setActive(false);
        teacher = teacherRepository.save(teacher);

        UserProfile profile = findProfileOrThrow(teacher.getProfileId());

        eventProducer.publishTeacherDeactivated(TeacherDeactivatedEvent.builder()
                .teacherId(teacher.getId())
                .userId(teacher.getUserId())
                .departmentId(teacher.getDepartment().getId())
                .build()
        );

        log.info("Enseignant désactivé : teacherId={}", teacherId);
        return userMapper.toTeacherResponse(teacher, profile);
    }

    @Override
    @Transactional
    public TeacherResponse reactivateTeacher(String teacherId) {
        Teacher teacher = findTeacherOrThrow(teacherId);

        if (teacher.isActive()) {
            throw new InvalidOperationException("L'enseignant est déjà actif");
        }

        teacher.setActive(true);
        teacher = teacherRepository.save(teacher);

        UserProfile profile = findProfileOrThrow(teacher.getProfileId());
        log.info("Enseignant réactivé : teacherId={}", teacherId);
        return userMapper.toTeacherResponse(teacher, profile);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Teacher findTeacherOrThrow(String teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Enseignant introuvable : id=" + teacherId
                ));
    }

    private UserProfile findProfileOrThrow(String profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Profil introuvable : id=" + profileId
                ));
    }

    private String generateEmployeeNumber() {
        int year = Year.now().getValue();
        long count = teacherRepository.count() + 1;
        return String.format("TCH-%d-%05d", year, count);
    }
}