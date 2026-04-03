package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateStudentGroupRequest;
import org.darius.course.dtos.requests.UpdateGroupStudentsRequest;
import org.darius.course.dtos.responses.StudentGroupResponse;
import org.darius.course.entities.Matiere;
import org.darius.course.entities.Semester;
import org.darius.course.entities.StudentGroup;
import org.darius.course.enums.GroupType;
import org.darius.course.events.published.StudentEnrolledEvent;
import org.darius.course.exceptions.*;
import org.darius.course.kafka.CourseEventProducer;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.MatiereRepository;
import org.darius.course.repositories.SemesterRepository;
import org.darius.course.repositories.StudentGroupRepository;
import org.darius.course.services.EnrollmentService;
import org.darius.course.services.StudentGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final SemesterRepository     semesterRepository;
    private final MatiereRepository      matiereRepository;
    private final EnrollmentService      enrollmentService;
    private final CourseMapper           mapper;
    private final CourseEventProducer    eventProducer;

    @Override
    @Transactional
    public StudentGroupResponse create(CreateStudentGroupRequest request) {
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Semestre introuvable : id=" + request.getSemesterId()));

        StudentGroup group = StudentGroup.builder()
                .name(request.getName())
                .type(request.getType())
                .levelId(request.getLevelId())
                .filiereId(request.getFiliereId())
                .semester(semester)
                .maxSize(request.getMaxSize())
                .studentIds(new ArrayList<>())
                .build();

        return mapper.toStudentGroupResponse(studentGroupRepository.save(group));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGroupResponse getById(Long id) {
        return mapper.toStudentGroupResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupResponse> getBySemester(Long semesterId) {
        return studentGroupRepository.findBySemester_Id(semesterId).stream()
                .map(mapper::toStudentGroupResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupResponse> getBySemesterAndType(Long semesterId, GroupType type) {
        return studentGroupRepository.findBySemester_IdAndType(semesterId, type).stream()
                .map(mapper::toStudentGroupResponse).toList();
    }

    @Override
    @Transactional
    public StudentGroupResponse updateStudents(Long id, UpdateGroupStudentsRequest request) {
        StudentGroup group = findOrThrow(id);

        if ("ADD".equalsIgnoreCase(request.getAction())) {

            // Récupérer les matières du niveau (pour enrollment + event)
            List<Matiere> matieres = matiereRepository.findByStudyLevelId(group.getLevelId());
            List<String>  matiereNames = matieres.stream().map(Matiere::getName).toList();
            List<Long>    matiereIds   = matieres.stream().map(Matiere::getId).toList();

            for (String studentId : request.getStudentIds()) {
                if (group.getStudentIds().contains(studentId)) {
                    log.warn("Étudiant {} déjà dans le groupe {}", studentId, group.getName());
                    continue;
                }
                if (group.getStudentIds().size() >= group.getMaxSize()) {
                    throw new InvalidOperationException(
                            "Le groupe " + group.getName() + " est plein (max=" + group.getMaxSize() + ")");
                }

                group.getStudentIds().add(studentId);

                // Créer les enrollments pour chaque matière du niveau
                enrollmentService.createEnrollmentsForStudent(
                        studentId, group.getLevelId(), group.getSemester().getId());

                // Publier l'événement d'inscription
                eventProducer.publishStudentEnrolled(
                        StudentEnrolledEvent.builder()
                                .studentId(studentId)
                                .semesterId(group.getSemester().getId())
                                .semesterLabel(group.getSemester().getLabel())
                                .academicYear(group.getSemester().getAcademicYear())
                                .matiereIds(matiereIds)
                                .matiereNames(matiereNames)
                                .build());

                log.info("Étudiant {} ajouté au groupe {} + enrollments créés + event publié",
                        studentId, group.getName());
            }

        } else if ("REMOVE".equalsIgnoreCase(request.getAction())) {
            group.getStudentIds().removeAll(request.getStudentIds());
        } else {
            throw new InvalidOperationException(
                    "Action invalide : " + request.getAction() + " (ADD ou REMOVE)");
        }

        return mapper.toStudentGroupResponse(studentGroupRepository.save(group));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getStudentIds(Long groupId) {
        return findOrThrow(groupId).getStudentIds();
    }

    @Override
    @Transactional
    public StudentGroupResponse findOrCreatePromoGroup(Long levelId, Long filiereId, Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Semestre introuvable : id=" + semesterId));

        return studentGroupRepository
                .findBySemester_IdAndFiliereIdAndLevelIdAndType(semesterId, filiereId, levelId, GroupType.PROMO)
                .map(mapper::toStudentGroupResponse)
                .orElseGet(() -> {
                    StudentGroup newGroup = StudentGroup.builder()
                            .name("Promo-L" + levelId + "-F" + filiereId + "-" + semester.getAcademicYear())
                            .type(GroupType.PROMO)
                            .levelId(levelId)
                            .filiereId(filiereId)
                            .semester(semester)
                            .maxSize(200)
                            .studentIds(new ArrayList<>())
                            .build();

                    log.info("Nouveau groupe PROMO créé : levelId={}, filiereId={}, semestre={}",
                            levelId, filiereId, semester.getLabel());

                    return mapper.toStudentGroupResponse(studentGroupRepository.save(newGroup));
                });
    }

    private StudentGroup findOrThrow(Long id) {
        return studentGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Groupe introuvable : id=" + id));
    }
}
