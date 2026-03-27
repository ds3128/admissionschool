package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.UpdateEnrollmentStatusRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.EnrollStatus;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository     enrollmentRepository;
    private final MatiereRepository        matiereRepository;
    private final SemesterRepository       semesterRepository;
    private final EvaluationRepository     evaluationRepository;
    private final CourseResourceRepository resourceRepository;
    private final AttendanceService        attendanceService;
    private final GradeService             gradeService;
    private final CourseMapper             mapper;

    @Override @Transactional(readOnly = true)
    public EnrollmentResponse getById(Long id) {
        return mapper.toEnrollmentResponse(enrollmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable")));
    }
    @Override @Transactional(readOnly = true)
    public List<EnrollmentResponse> getByStudentAndSemester(String sid, Long semId) {
        return enrollmentRepository.findByStudentIdAndSemester_Id(sid, semId).stream().map(mapper::toEnrollmentResponse).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<EnrollmentResponse> getByMatiereAndSemester(Long mid, Long semId) {
        return enrollmentRepository.findByMatiere_IdAndSemester_Id(mid, semId).stream().map(mapper::toEnrollmentResponse).toList();
    }
    @Override @Transactional
    public EnrollmentResponse updateStatus(Long id, UpdateEnrollmentStatusRequest req) {
        Enrollment e = enrollmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable"));
        e.setStatus(req.getStatus()); return mapper.toEnrollmentResponse(enrollmentRepository.save(e));
    }
    @Override @Transactional
    public void createEnrollmentsForStudent(String studentId, Long levelId, Long semesterId) {
        Semester sem = semesterRepository.findById(semesterId).orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable"));
        matiereRepository.findByStudyLevelId(levelId).forEach(m -> {
            if (!enrollmentRepository.existsByStudentIdAndMatiere_IdAndSemester_Id(studentId, m.getId(), semesterId))
                enrollmentRepository.save(Enrollment.builder().studentId(studentId).matiere(m).semester(sem)
                        .enrolledAt(LocalDate.now()).status(EnrollStatus.ACTIVE).build());
        });
    }
    @Override @Transactional
    public void blockStudentEnrollments(String studentId, Long semesterId) {
        enrollmentRepository.findByStudentIdAndSemester_IdAndStatus(studentId, semesterId, EnrollStatus.ACTIVE)
                .forEach(e -> { e.setStatus(EnrollStatus.BLOCKED); enrollmentRepository.save(e); });
        log.info("Enrollments bloqués pour étudiant {} (impayé)", studentId);
    }
    @Override @Transactional(readOnly = true)
    public List<CourseDashboardEntry> getStudentDashboard(String studentId, Long semesterId) {
        return enrollmentRepository.findByStudentIdAndSemester_Id(studentId, semesterId).stream().map(e -> {
            Matiere m = e.getMatiere();
            List<GradeResponse> grades = gradeService.getByStudentAndMatiereAndSemester(studentId, m.getId(), semesterId);
            Double avg = grades.isEmpty() ? null : grades.stream()
                    .mapToDouble(g -> g.getMaxScore() > 0 ? (g.getScore() / g.getMaxScore()) * 20.0 * g.getCoefficient() : 0)
                    .sum() / grades.stream().mapToDouble(GradeResponse::getCoefficient).sum();
            AttendanceStatsResponse att = attendanceService.getStatsByStudentAndMatiereAndSemester(studentId, m.getId(), semesterId);
            List<EvaluationResponse> upcoming = evaluationRepository.findUpcomingByMatiereAndSemester(m.getId(), semesterId, LocalDate.now())
                    .stream().map(mapper::toEvaluationResponse).toList();
            List<CourseResourceResponse> recent = resourceRepository.findTop3PublishedByMatiereId(m.getId())
                    .stream().map(mapper::toCourseResourceResponse).toList();
            long resCount = resourceRepository.countByMatiere_IdAndIsPublishedTrueAndIsDeletedFalse(m.getId());
            return CourseDashboardEntry.builder().matiereId(m.getId()).matiereName(m.getName())
                    .teachingUnitName(m.getTeachingUnit().getName()).coefficient(m.getCoefficient())
                    .currentAverage(avg != null ? Math.round(avg * 100.0) / 100.0 : null)
                    .attendanceRate(att.getAttendanceRate()).enrollStatus(e.getStatus())
                    .upcomingEvaluations(upcoming).resourceCount((int)resCount).recentResources(recent).build();
        }).toList();
    }
}
