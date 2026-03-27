package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.JustifyAbsenceRequest;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.EnrollStatus;
import org.darius.course.events.published.AttendanceThresholdExceededEvent;
import org.darius.course.exceptions.*;
import org.darius.course.kafka.CourseEventProducer;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.AttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MatiereRepository    matiereRepository;
    private final CourseEventProducer  eventProducer;
    private final CourseMapper         mapper;

    @Override @Transactional(readOnly = true)
    public AttendanceResponse getById(Long id) {
        return mapper.toAttendanceResponse(attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Présence introuvable")));
    }

    @Override @Transactional(readOnly = true)
    public List<AttendanceResponse> getBySession(Long sid) {
        return attendanceRepository.findBySession_Id(sid).stream().map(mapper::toAttendanceResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<AttendanceStatsResponse> getStatsByStudentAndSemester(String studentId, Long semesterId) {
        return enrollmentRepository.findByStudentIdAndSemester_IdAndStatus(studentId, semesterId, EnrollStatus.ACTIVE).stream()
                .map(e -> buildStats(studentId, e.getMatiere().getId(), e.getMatiere().getName(), semesterId, e.getMatiere().getAttendanceThreshold())).toList();
    }

    @Override @Transactional(readOnly = true)
    public AttendanceStatsResponse getStatsByStudentAndMatiereAndSemester(String studentId, Long matiereId, Long semesterId) {
        Matiere m = matiereRepository.findById(matiereId).orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));
        return buildStats(studentId, matiereId, m.getName(), semesterId, m.getAttendanceThreshold());
    }

    @Override @Transactional
    public AttendanceResponse justify(Long id, JustifyAbsenceRequest req) {
        Attendance att = attendanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Présence introuvable"));
        if (att.isPresent()) throw new InvalidOperationException("L'étudiant était présent — justification inutile");
        att.setJustification(req.getJustification()); att.setJustifiedAt(LocalDateTime.now());
        return mapper.toAttendanceResponse(attendanceRepository.save(att));
    }

    @Override @Transactional
    public void checkThreshold(String studentId, Long matiereId, Long semesterId) {
        Matiere m = matiereRepository.findById(matiereId).orElse(null);
        if (m == null) return;
        AttendanceStatsResponse stats = buildStats(studentId, matiereId, m.getName(), semesterId, m.getAttendanceThreshold());
        if (stats.getTotalSessions() == 0) return;
        if (stats.getAttendanceRate() < m.getAttendanceThreshold()) {
            enrollmentRepository.findByStudentIdAndMatiere_IdAndSemester_Id(studentId, matiereId, semesterId).ifPresent(e -> {
                if (e.getStatus() != EnrollStatus.BLOCKED) {
                    e.setStatus(EnrollStatus.BLOCKED); enrollmentRepository.save(e);
                    eventProducer.publishAttendanceThresholdExceeded(AttendanceThresholdExceededEvent.builder()
                            .studentId(studentId).matiereId(matiereId).matiereName(m.getName()).semesterId(semesterId)
                            .attendanceRate(stats.getAttendanceRate()).threshold(m.getAttendanceThreshold())
                            .totalSessions(stats.getTotalSessions()).absenceCount(stats.getAbsenceCount()).build());
                    log.warn("Étudiant {} bloqué — absences dans {} : {}%", studentId, m.getName(), stats.getAttendanceRate());
                }
            });
        }
    }

    @Override @Transactional
    public void checkAllThresholds() {
        log.info("Scheduler : vérification seuils absences");
        enrollmentRepository.findAll().stream().filter(e -> e.getStatus() == EnrollStatus.ACTIVE)
                .forEach(e -> checkThreshold(e.getStudentId(), e.getMatiere().getId(), e.getSemester().getId()));
    }

    private AttendanceStatsResponse buildStats(String studentId, Long matiereId, String name, Long semesterId, double threshold) {
        long present  = attendanceRepository.countByStudentAndMatiereAndSemesterAndPresent(studentId, matiereId, semesterId, true);
        long absent   = attendanceRepository.countByStudentAndMatiereAndSemesterAndPresent(studentId, matiereId, semesterId, false);
        int  total    = (int)(present + absent);
        double rate   = total > 0 ? Math.round((present * 100.0 / total) * 10.0) / 10.0 : 0.0;
        long justified = attendanceRepository.findByStudentAndMatiereAndSemester(studentId, matiereId, semesterId)
                .stream().filter(a -> !a.isPresent() && a.getJustification() != null && !a.getJustification().isBlank()).count();
        return AttendanceStatsResponse.builder().studentId(studentId).matiereId(matiereId).matiereName(name)
                .semesterId(semesterId).totalSessions(total).presentCount((int)present).absenceCount((int)absent)
                .justifiedCount((int)justified).attendanceRate(rate).blocked(rate < threshold && total > 0).build();
    }
}
