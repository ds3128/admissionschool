package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.SubmitGradesRequest;
import org.darius.course.dtos.responses.GradeResponse;
import org.darius.course.entities.*;
import org.darius.course.enums.SemesterStatus;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class GradeServiceImpl implements GradeService {

    private final GradeRepository          gradeRepository;
    private final EvaluationRepository     evaluationRepository;
    private final TeacherAssignmentService assignmentService;
    private final CourseMapper             mapper;

    @Override @Transactional
    public List<GradeResponse> submitGrades(Long evalId, String teacherId, SubmitGradesRequest req) {
        Evaluation ev = evaluationRepository.findById(evalId).orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable"));
        if (ev.getSemester().getStatus() != SemesterStatus.ACTIVE)
            throw new InvalidOperationException("Saisie de notes uniquement sur semestre ACTIVE");
        if (!assignmentService.isTeacherAssigned(teacherId, ev.getMatiere().getId(), ev.getSemester().getId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");
        Map<String,String> errors = new HashMap<>();
        req.getGrades().forEach(e -> { if (e.getScore() < 0 || e.getScore() > ev.getMaxScore())
            errors.put(e.getStudentId(), "Score invalide : " + e.getScore() + " (max=" + ev.getMaxScore() + ")"); });
        if (!errors.isEmpty()) throw new InvalidOperationException("Erreurs de validation : " + errors);
        List<GradeResponse> responses = new ArrayList<>();
        req.getGrades().forEach(entry -> {
            Grade g = gradeRepository.findByStudentIdAndEvaluation_Id(entry.getStudentId(), evalId)
                    .orElse(Grade.builder().studentId(entry.getStudentId()).evaluation(ev)
                            .matiere(ev.getMatiere()).semester(ev.getSemester()).build());
            g.setScore(entry.getScore()); g.setComment(entry.getComment()); g.setGradedBy(teacherId);
            responses.add(mapper.toGradeResponse(gradeRepository.save(g)));
        });
        return responses;
    }

    @Override @Transactional
    public GradeResponse updateGrade(Long gradeId, String teacherId, double score, String comment) {
        Grade g = gradeRepository.findById(gradeId).orElseThrow(() -> new ResourceNotFoundException("Note introuvable"));
        if (g.getSemester().getStatus() == SemesterStatus.VALIDATED)
            throw new InvalidOperationException("Note immuable dans un semestre VALIDATED");
        if (score < 0 || score > g.getEvaluation().getMaxScore())
            throw new InvalidOperationException("Score invalide : " + score);
        g.setScore(score); g.setComment(comment); g.setGradedBy(teacherId);
        return mapper.toGradeResponse(gradeRepository.save(g));
    }

    @Override @Transactional(readOnly = true)
    public GradeResponse getById(Long id) {
        return mapper.toGradeResponse(gradeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Note introuvable")));
    }
    @Override @Transactional(readOnly = true)
    public List<GradeResponse> getByStudentAndSemester(String sid, Long semId) {
        return gradeRepository.findByStudentIdAndSemester_Id(sid, semId).stream().map(mapper::toGradeResponse).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<GradeResponse> getByStudentAndMatiereAndSemester(String sid, Long mid, Long semId) {
        return gradeRepository.findByStudentIdAndMatiere_IdAndSemester_Id(sid, mid, semId).stream().map(mapper::toGradeResponse).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<GradeResponse> getByEvaluation(Long eid) {
        return gradeRepository.findByEvaluation_Id(eid).stream().map(mapper::toGradeResponse).toList();
    }
}
