package org.darius.userservice.services.impl;

import org.darius.userservice.common.dtos.requests.CreateFiliereRequest;
import org.darius.userservice.common.dtos.requests.UpdateFiliereStatusRequest;
import org.darius.userservice.common.dtos.responses.FiliereResponse;
import org.darius.userservice.common.dtos.responses.StudyLevelResponse;
import org.darius.userservice.common.enums.FiliereStatus;
import org.darius.userservice.mappers.UserMapper;
import org.darius.userservice.services.FiliereService;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.darius.userservice.entities.Department;
import org.darius.userservice.entities.Filiere;
import org.darius.userservice.entities.StudyLevel;
import org.darius.userservice.exceptions.DuplicateResourceException;
import org.darius.userservice.exceptions.InvalidOperationException;
import org.darius.userservice.exceptions.UserNotFoundException;
import org.darius.userservice.repositories.DepartmentRepository;
import org.darius.userservice.repositories.FiliereRepository;
import org.darius.userservice.repositories.StudyLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Slf4j
public class FiliereServiceImpl implements FiliereService {

    private final FiliereRepository filiereRepository;
    private final DepartmentRepository departmentRepository;
    private final StudyLevelRepository studyLevelRepository;
    private final UserMapper userMapper;

    public FiliereServiceImpl(FiliereRepository filiereRepository, DepartmentRepository departmentRepository, StudyLevelRepository studyLevelRepository, UserMapper userMapper) {
        this.filiereRepository = filiereRepository;
        this.departmentRepository = departmentRepository;
        this.studyLevelRepository = studyLevelRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public FiliereResponse createFiliere(CreateFiliereRequest request) {
        log.info("Création de la filière : code={}", request.getCode());

        if (filiereRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Une filière avec le code '" + request.getCode() + "' existe déjà"
            );
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new UserNotFoundException(
                        "Département introuvable : id=" + request.getDepartmentId()
                ));

        Filiere filiere = Filiere.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .department(department)
                .durationYears(request.getDurationYears())
                .description(request.getDescription())
                .status(FiliereStatus.ACTIVE)
                .studyLevels(new ArrayList<>())
                .students(new ArrayList<>())
                .build();

        filiere = filiereRepository.save(filiere);

        // Génération automatique des StudyLevel (N = durationYears)
        List<StudyLevel> levels = generateStudyLevels(filiere);
        filiere.setStudyLevels(levels);

        log.info("Filière créée : id={}, code={}, {} niveaux générés",
                filiere.getId(), filiere.getCode(), levels.size());

        return userMapper.toFiliereResponse(filiere);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiliereResponse> getAllFilieres(FiliereStatus status) {
        List<Filiere> filieres = (status != null)
                ? filiereRepository.findByStatus(status)
                : filiereRepository.findAll();
        return userMapper.toFiliereResponseList(filieres);
    }

    @Override
    @Transactional(readOnly = true)
    public FiliereResponse getFiliereById(Long filiereId) {
        return userMapper.toFiliereResponse(findFiliereOrThrow(filiereId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyLevelResponse> getStudyLevelsByFiliere(Long filiereId) {
        findFiliereOrThrow(filiereId); // vérifie l'existence
        return userMapper.toStudyLevelResponseList(
                studyLevelRepository.findByFiliere_IdOrderByOrder(filiereId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StudyLevelResponse getStudyLevelById(Long levelId) {
        return userMapper.toStudyLevelResponse(findStudyLevelOrThrow(levelId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudyLevelResponse getFirstLevelByFiliere(Long filiereId) {
        return userMapper.toStudyLevelResponse(
                studyLevelRepository.findByFiliere_IdAndOrder(filiereId, 1)
                        .orElseThrow(() -> new UserNotFoundException(
                                "Aucun niveau trouvé pour la filière id=" + filiereId
                        ))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StudyLevelResponse getNextLevel(Long currentLevelId) {
        return studyLevelRepository.findNextLevel(currentLevelId)
                .map(userMapper::toStudyLevelResponse)
                .orElse(null); // null = étudiant déjà au dernier niveau
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLastLevel(Long levelId) {
        return studyLevelRepository.isLastLevel(levelId);
    }

    @Override
    @Transactional
    public FiliereResponse updateFiliereStatus(Long filiereId, UpdateFiliereStatusRequest request) {
        Filiere filiere = findFiliereOrThrow(filiereId);
        FiliereStatus newStatus = request.getStatus();

        validateStatusTransition(filiere, newStatus);

        log.info("Changement de statut filière : id={}, {} → {}",
                filiereId, filiere.getStatus(), newStatus);

        filiere.setStatus(newStatus);
        return userMapper.toFiliereResponse(filiereRepository.save(filiere));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<StudyLevel> generateStudyLevels(Filiere filiere) {
        List<StudyLevel> levels = new ArrayList<>();
        int duration = filiere.getDurationYears();

        // Préfixe selon la durée : L pour 3 ans, M pour 2 ans, D pour 3 ans
        String prefix = resolvePrefix(duration);

        for (int i = 1; i <= duration; i++) {
            StudyLevel level = StudyLevel.builder()
                    .label(prefix + " " + i)
                    .code(prefix.charAt(0) + String.valueOf(i))  // ex: L1, L2, M1, D1
                    .order(i)
                    .filiere(filiere)
                    .build();
            levels.add(studyLevelRepository.save(level));
        }
        return levels;
    }

    private String resolvePrefix(int durationYears) {
        return switch (durationYears) {
            case 1, 2, 3 -> "Licence";
            case 4, 5 -> "Master";
            default -> "Niveau";
        };
    }

    private void validateStatusTransition(Filiere filiere, FiliereStatus newStatus) {
        FiliereStatus current = filiere.getStatus();

        // ARCHIVED est irréversible
        if (current == FiliereStatus.ARCHIVED) {
            throw new InvalidOperationException(
                    "Une filière archivée ne peut pas changer de statut"
            );
        }

        // On ne peut pas archiver une filière avec des étudiants actifs
        if (newStatus == FiliereStatus.ARCHIVED
                && filiereRepository.hasActiveStudents(filiere.getId())) {
            throw new InvalidOperationException(
                    "Impossible d'archiver une filière avec des étudiants actifs"
            );
        }
    }

    private Filiere findFiliereOrThrow(Long filiereId) {
        return filiereRepository.findById(filiereId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Filière introuvable : id=" + filiereId
                ));
    }

    private StudyLevel findStudyLevelOrThrow(Long levelId) {
        return studyLevelRepository.findById(levelId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Niveau d'étude introuvable : id=" + levelId
                ));
    }
}