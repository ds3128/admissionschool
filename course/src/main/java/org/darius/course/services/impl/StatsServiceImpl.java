package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.StudentProgress;
import org.darius.course.enums.EnrollStatus;
import org.darius.course.repositories.*;
import org.darius.course.services.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class StatsServiceImpl implements StatsService {

    private final EnrollmentRepository      enrollmentRepository;
    private final EvaluationRepository      evaluationRepository;
    private final StudentProgressRepository progressRepository;
    private final AttendanceService         attendanceService;
    private final EvaluationService         evaluationService;

    @Override @Transactional(readOnly = true)
    public List<AttendanceStatsResponse> getAttendanceStatsForML(Long semesterId, Long filiereId) {
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getSemester().getId().equals(semesterId) && e.getStatus() == EnrollStatus.ACTIVE)
                .map(e -> attendanceService.getStatsByStudentAndMatiereAndSemester(e.getStudentId(), e.getMatiere().getId(), semesterId))
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public List<ClassStatsResponse> getGradeStatsForML(Long semesterId, Long matiereId) {
        return evaluationRepository.findByMatiere_IdAndSemester_Id(matiereId, semesterId).stream()
                .map(e -> evaluationService.getStats(e.getId())).toList();
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> getProgressSummaryForML(Long semesterId, Long groupId) {
        var progresses = progressRepository.findBySemester_Id(semesterId);
        if (progresses.isEmpty()) return Map.of("count", 0);
        double avg = progresses.stream().mapToDouble(StudentProgress::getSemesterAverage).average().orElse(0.0);
        double min = progresses.stream().mapToDouble(StudentProgress::getSemesterAverage).min().orElse(0.0);
        double max = progresses.stream().mapToDouble(StudentProgress::getSemesterAverage).max().orElse(0.0);
        double stdDev = Math.sqrt(progresses.stream().mapToDouble(p -> Math.pow(p.getSemesterAverage() - avg, 2)).average().orElse(0.0));
        long passing = progresses.stream().filter(StudentProgress::isAdmis).count();
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("semesterId", semesterId); result.put("count", progresses.size());
        result.put("average",  Math.round(avg*100.0)/100.0); result.put("min", Math.round(min*100.0)/100.0);
        result.put("max",      Math.round(max*100.0)/100.0); result.put("stdDev", Math.round(stdDev*100.0)/100.0);
        result.put("passRate", Math.round((passing*100.0/progresses.size())*10.0)/10.0);
        return result;
    }
}
