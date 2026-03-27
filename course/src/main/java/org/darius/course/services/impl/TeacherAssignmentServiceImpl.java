package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateTeacherAssignmentRequest;
import org.darius.course.dtos.responses.MatiereResponse;
import org.darius.course.dtos.responses.TeacherAssignmentResponse;
import org.darius.course.dtos.responses.TeacherLoadResponse;
import org.darius.course.entities.Matiere;
import org.darius.course.entities.Semester;
import org.darius.course.entities.TeacherAssignment;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.MatiereRepository;
import org.darius.course.repositories.SemesterRepository;
import org.darius.course.repositories.TeacherAssignmentRepository;
import org.darius.course.services.TeacherAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final TeacherAssignmentRepository assignmentRepository;
    private final MatiereRepository           matiereRepository;
    private final SemesterRepository          semesterRepository;
    private final CourseMapper                mapper;
    private final RestClient                  restClient;

    @Override
    @Transactional
    public TeacherAssignmentResponse create(CreateTeacherAssignmentRequest request) {
        // Vérifier doublon
        if (assignmentRepository.existsByTeacherIdAndMatiere_IdAndRoleAndSemester_Id(
                request.getTeacherId(), request.getMatiereId(),
                request.getRole(), request.getSemesterId()
        )) {
            throw new DuplicateResourceException(
                    "Cet enseignant est déjà affecté à cette matière avec ce rôle pour ce semestre"
            );
        }

        Matiere matiere = matiereRepository.findById(request.getMatiereId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matière introuvable : id=" + request.getMatiereId()
                ));

        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Semestre introuvable : id=" + request.getSemesterId()
                ));

        // Vérifier la charge horaire max via le User Service
        verifyTeacherHoursLimit(request.getTeacherId(), request.getSemesterId(), request.getAssignedHours());

        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacherId(request.getTeacherId())
                .matiere(matiere)
                .role(request.getRole())
                .semester(semester)
                .assignedHours(request.getAssignedHours())
                .build();

        return mapper.toTeacherAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeacherAssignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Affectation introuvable : id=" + id));
        assignmentRepository.delete(assignment);
        log.info("Affectation {} supprimée", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getByTeacherAndSemester(String teacherId, Long semesterId) {
        return assignmentRepository.findByTeacherIdAndSemester_Id(teacherId, semesterId).stream()
                .map(mapper::toTeacherAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getByMatiereAndSemester(Long matiereId, Long semesterId) {
        return assignmentRepository.findByMatiere_IdAndSemester_Id(matiereId, semesterId).stream()
                .map(mapper::toTeacherAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherLoadResponse getTeacherLoad(String teacherId, Long semesterId) {
        List<TeacherAssignment> assignments =
                assignmentRepository.findByTeacherIdAndSemester_Id(teacherId, semesterId);

        int cmHours = 0, tdHours = 0, tpHours = 0;
        for (TeacherAssignment a : assignments) {
            switch (a.getRole()) {
                case CM -> cmHours += a.getAssignedHours();
                case TD -> tdHours += a.getAssignedHours();
                case TP -> tpHours += a.getAssignedHours();
            }
        }

        List<MatiereResponse> matieres = assignments.stream()
                .map(a -> mapper.toMatiereResponse(a.getMatiere()))
                .distinct()
                .toList();

        Semester semester = semesterRepository.findById(semesterId).orElse(null);

        return TeacherLoadResponse.builder()
                .teacherId(teacherId)
                .semesterId(semesterId)
                .semesterLabel(semester != null ? semester.getLabel() : "")
                .totalHours(cmHours + tdHours + tpHours)
                .cmHours(cmHours)
                .tdHours(tdHours)
                .tpHours(tpHours)
                .matieres(matieres)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacherAssigned(String teacherId, Long matiereId, Long semesterId) {
        return assignmentRepository.isTeacherAssignedToMatiere(teacherId, matiereId, semesterId);
    }

    // ── Vérification charge horaire via User Service ───────────────────────────

    private void verifyTeacherHoursLimit(String teacherId, Long semesterId, int newHours) {
        try {
            // Appel HTTP synchrone vers le User Service
            Map teacherInfo = restClient.get()
                    .uri("http://localhost:8082/users/teachers/" + teacherId)
                    .retrieve()
                    .body(Map.class);

            if (teacherInfo == null) {
                log.warn("Enseignant {} non trouvé dans le User Service — vérification ignorée", teacherId);
                return;
            }

            int maxHoursPerWeek = teacherInfo.containsKey("maxHoursPerWeek")
                    ? (Integer) teacherInfo.get("maxHoursPerWeek")
                    : 18; // défaut standard

            // Heures déjà assignées pour ce semestre
            int currentHours = assignmentRepository
                    .sumAssignedHoursByTeacherAndSemester(teacherId, semesterId);

            int totalAfter = currentHours + newHours;
            int weeksInSemester = 16; // estimation standard
            int maxTotalHours = maxHoursPerWeek * weeksInSemester;

            if (totalAfter > maxTotalHours) {
                throw new InvalidOperationException(
                        "Dépassement de la charge horaire de l'enseignant " + teacherId
                                + " : " + totalAfter + "h assignées / " + maxTotalHours + "h max"
                                + " (charge actuelle : " + currentHours + "h, ajout demandé : " + newHours + "h)"
                );
            }

        } catch (InvalidOperationException e) {
            throw e;
        } catch (Exception ex) {
            // Si le User Service est indisponible, on continue avec un avertissement
            log.warn("User Service indisponible pour vérification charge enseignant {} : {}",
                    teacherId, ex.getMessage());
        }
    }
}