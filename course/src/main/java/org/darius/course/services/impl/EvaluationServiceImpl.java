package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateEvaluationRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.EnrollStatus;
import org.darius.course.events.published.GradesPublishedEvent;
import org.darius.course.exceptions.*;
import org.darius.course.kafka.CourseEventProducer;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository     evaluationRepository;
    private final MatiereRepository        matiereRepository;
    private final SemesterRepository       semesterRepository;
    private final GradeRepository          gradeRepository;
    private final EnrollmentRepository     enrollmentRepository;
    private final TeacherAssignmentService assignmentService;
    private final CourseEventProducer      eventProducer;
    private final CourseMapper             mapper;

    @Override @Transactional
    public EvaluationResponse create(String teacherId, CreateEvaluationRequest req) {
        Matiere  m   = matiereRepository.findById(req.getMatiereId())
                .orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));

        Semester sem = semesterRepository.findById(req.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable"));

        if (!assignmentService.isTeacherAssigned(teacherId, req.getMatiereId(), req.getSemesterId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");

        double remaining = getRemainingCoefficient(req.getMatiereId(), req.getSemesterId());

        if (req.getCoefficient() > remaining + 0.001)
            throw new InvalidOperationException("Coefficient trop élevé — solde : " + String.format("%.2f", remaining));

        return mapper.toEvaluationResponse(
                evaluationRepository.save(
                        Evaluation.builder()
                            .title(req.getTitle())
                            .type(req.getType())
                            .matiere(m)
                            .semester(sem)
                            .date(req.getDate())
                            .coefficient(req.getCoefficient())
                            .maxScore(req.getMaxScore())
                            .isPublished(false)
                            .build()
                )
        );
    }

    @Override @Transactional
    public EvaluationResponse update(Long id, String teacherId, CreateEvaluationRequest req) {
        Evaluation ev = findOrThrow(id);
        if (!assignmentService.isTeacherAssigned(teacherId, ev.getMatiere().getId(), ev.getSemester().getId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");
        double used = evaluationRepository.sumCoefficientsByMatiereAndSemester(ev.getMatiere().getId(), ev.getSemester().getId());
        double available = 1.0 - (used - ev.getCoefficient());
        if (req.getCoefficient() > available + 0.001)
            throw new InvalidOperationException("Coefficient trop élevé — solde : " + String.format("%.2f", available));
        ev.setTitle(req.getTitle()); ev.setType(req.getType()); ev.setDate(req.getDate());
        ev.setCoefficient(req.getCoefficient()); ev.setMaxScore(req.getMaxScore());
        return mapper.toEvaluationResponse(evaluationRepository.save(ev));
    }

    @Override @Transactional
    public void delete(Long id, String teacherId) {
        Evaluation ev = findOrThrow(id);
        if (!gradeRepository.findByEvaluation_Id(id).isEmpty())
            throw new InvalidOperationException("Impossible de supprimer une évaluation avec des notes existantes");
        evaluationRepository.delete(ev);
    }

    @Override @Transactional(readOnly = true)
    public EvaluationResponse getById(Long id) { return mapper.toEvaluationResponse(findOrThrow(id)); }

    @Override @Transactional(readOnly = true)
    public List<EvaluationResponse> getByMatiereAndSemester(Long mid, Long sid) {
        return evaluationRepository.findByMatiere_IdAndSemester_Id(mid, sid).stream().map(mapper::toEvaluationResponse).toList();
    }

    @Override @Transactional
    public EvaluationResponse publish(Long id, String teacherId) {

        Evaluation ev = findOrThrow(id);

        if (!assignmentService.isTeacherAssigned(teacherId, ev.getMatiere().getId(), ev.getSemester().getId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");

        ev.setPublished(true);

        evaluationRepository.save(ev);

        List<String> studentIds = enrollmentRepository.findByMatiere_IdAndSemester_IdAndStatus(
                ev.getMatiere().getId(),
                ev.getSemester().getId(),
                EnrollStatus.ACTIVE).stream().map(Enrollment::getStudentId).toList();

        eventProducer.publishGradesPublished(
            GradesPublishedEvent.builder()
                .evaluationId(ev.getId())
                .evaluationTitle(ev.getTitle())
                .matiereId(ev.getMatiere().getId())
                .matiereName(ev.getMatiere().getName())
                .semesterId(ev.getSemester().getId())
                .studentIds(studentIds)
                .build()
        );
        return mapper.toEvaluationResponse(ev);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassStatsResponse getStats(Long evaluationId) {
        Evaluation eval = findOrThrow(evaluationId);

        // ✅ Récupérer le premier élément de la liste
        List<Object[]> rawList = gradeRepository.computeStatsForEvaluation(evaluationId);

        if (rawList == null || rawList.isEmpty() || rawList.getFirst()[0] == null) {
            return ClassStatsResponse.builder()
                    .evaluationId(evaluationId)
                    .evaluationTitle(eval.getTitle())
                    .matiereId(eval.getMatiere().getId())
                    .matiereName(eval.getMatiere().getName())
                    .average(0.0).min(0.0).max(0.0)
                    .passRate(0.0).standardDeviation(0.0)
                    .totalStudents(0)
                    .build();
        }

        Object[] raw = rawList.getFirst();

        double min   = ((Number) raw[0]).doubleValue();
        double max   = ((Number) raw[1]).doubleValue();
        double avg   = ((Number) raw[2]).doubleValue();
        long   count = ((Number) raw[3]).longValue();

        // Écart-type et taux de réussite
        List<Grade> grades = gradeRepository.findByEvaluation_Id(evaluationId);

        double variance = grades.stream()
                .mapToDouble(g -> Math.pow(g.getScore() - avg, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        long passing = grades.stream()
                .filter(g -> (g.getScore() / eval.getMaxScore()) >= 0.5)
                .count();
        double passRate = count > 0 ? (double) passing / count * 100.0 : 0.0;

        return ClassStatsResponse.builder()
                .evaluationId(evaluationId)
                .evaluationTitle(eval.getTitle())
                .matiereId(eval.getMatiere().getId())
                .matiereName(eval.getMatiere().getName())
                .average(Math.round(avg * 100.0) / 100.0)
                .min(min).max(max)
                .passRate(Math.round(passRate * 100.0) / 100.0)
                .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                .totalStudents((int) count)
                .build();
    }

    @Override @Transactional(readOnly = true)
    public double getRemainingCoefficient(Long matiereId, Long semesterId) {
        return Math.max(0.0, 1.0 - evaluationRepository.sumCoefficientsByMatiereAndSemester(matiereId, semesterId));
    }

    private Evaluation findOrThrow(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable : id=" + id));
    }
}