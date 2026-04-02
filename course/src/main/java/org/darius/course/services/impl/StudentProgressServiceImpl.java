package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.responses.*;
import org.darius.course.entities.*;
import org.darius.course.enums.*;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.StudentProgressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.util.*; import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class StudentProgressServiceImpl implements StudentProgressService {

    private final StudentProgressRepository progressRepository;
    private final EnrollmentRepository      enrollmentRepository;
    private final GradeRepository           gradeRepository;
    private final SemesterRepository        semesterRepository;
    private final TeachingUnitRepository    teachingUnitRepository;
    private final CourseMapper              mapper;

    @Override @Transactional(readOnly = true)
    public StudentProgressResponse getByStudentAndSemester(String sid, Long semId) {
        return mapper.toStudentProgressResponse(progressRepository.findByStudentIdAndSemester_Id(sid, semId)
                .orElseThrow(() -> new ResourceNotFoundException("Progression introuvable")));
    }
    @Override @Transactional(readOnly = true)
    public List<StudentProgressResponse> getByStudentId(String sid) {
        return progressRepository.findByStudentId(sid).stream().map(mapper::toStudentProgressResponse).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<StudentProgressResponse> getBySemester(Long semId) {
        return progressRepository.findBySemester_Id(semId).stream().map(mapper::toStudentProgressResponse).toList();
    }

    @Override @Transactional
    public StudentProgressResponse computeForStudent(String studentId, Long semesterId) {

        Semester sem = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable"));

        List<Grade> grades = gradeRepository.findAllByStudentAndSemester(studentId, semesterId);

        Map<Long, List<Grade>> byMatiere = grades.stream().collect(Collectors.groupingBy(g -> g.getMatiere().getId()));

        Map<Long, Double> matiereScores = new HashMap<>();

        byMatiere.forEach((mid, gl) -> {
            double ws = 0, tc = 0;
            for (Grade g : gl) { double s20 = (g.getScore() / g.getEvaluation().getMaxScore()) * 20.0; ws += s20 * g.getEvaluation().getCoefficient();
                tc += g.getEvaluation().getCoefficient();
            }
            matiereScores.put(mid, tc > 0 ? ws / tc : 0.0);
        });

        double semWS = 0, semTC = 0; int credits = 0;
        for (TeachingUnit ue : teachingUnitRepository.findAll()) {
            double ueWS = 0, ueTC = 0;
            for (Matiere m : ue.getMatieres()) {
                if (matiereScores.containsKey(m.getId())) { ueWS += matiereScores.get(m.getId()) * m.getCoefficient(); ueTC += m.getCoefficient(); }
            }
            if (ueTC > 0) {
                double ueAvg = ueWS / ueTC; semWS += ueAvg * ue.getCoefficient();
                semTC += ue.getCoefficient();
                if (ueAvg >= 10.0) credits += ue.getCredits();
            }
        }

        double avg = semTC > 0 ? Math.round((semWS / semTC) * 100.0) / 100.0 : 0.0;
        ProgressStatus status  = avg >= 10.0 ? ProgressStatus.ADMIS : ProgressStatus.AJOURNE;
        Mention        mention = avg >= 16.0 ? Mention.TRES_BIEN : avg >= 14.0 ? Mention.BIEN : avg >= 12.0 ? Mention.ASSEZ_BIEN : avg >= 10.0 ? Mention.PASSABLE : Mention.INSUFFISANT;

        StudentProgress p = progressRepository.findByStudentIdAndSemester_Id(studentId, semesterId)
                .orElse(StudentProgress.builder().studentId(studentId).semester(sem).build());

        p.setSemesterAverage(avg); p.setCreditsObtained(credits); p.setStatus(status);

        p.setMention(mention); p.setAdmis(status == ProgressStatus.ADMIS); p.setComputedAt(LocalDateTime.now());

        return mapper.toStudentProgressResponse(progressRepository.save(p));
    }

    @Override @Transactional
    public List<StudentProgressResponse> computeAllForSemester(Long semesterId) {
        List<String> students = enrollmentRepository.findAll().stream()
                .filter(e -> e.getSemester().getId().equals(semesterId) && e.getStatus() == EnrollStatus.ACTIVE)
                .map(Enrollment::getStudentId).distinct().toList();

        List<StudentProgressResponse> results = new ArrayList<>();
        for (String sid : students) results.add(computeForStudent(sid, semesterId));

        // Calcul des rangs
        results.sort(Comparator.comparingDouble(StudentProgressResponse::getSemesterAverage).reversed());
        for (int i = 0; i < results.size(); i++) {
            final int rank = i + 1;
            final String sid = results.get(i).getStudentId();
            progressRepository.findByStudentIdAndSemester_Id(sid, semesterId).ifPresent(p -> {
                p.setRank(rank);
                progressRepository.save(p);
            });
        }
        log.info("Progressions calculées - semestre {} : {} étudiants", semesterId, results.size());
        return results;
    }

    @Override @Transactional(readOnly = true)
    public TranscriptResponse getTranscript(String studentId, String academicYear, Long semesterId) {
        Semester sem = semesterId != null ? semesterRepository.findById(semesterId).orElse(null) : null;
        List<Grade> grades = semesterId != null ? gradeRepository.findAllByStudentAndSemester(studentId, semesterId) : List.of();
        Map<Long, List<Grade>> byMatiere = grades.stream().collect(Collectors.groupingBy(g -> g.getMatiere().getId()));

        List<UEResultResponse> ueResults = new ArrayList<>();
        double genAvg = 0, totalW = 0;
        int totalCredits = 0;

        for (TeachingUnit ue : teachingUnitRepository.findAll()) {
            List<MatiereResultResponse> mRes = new ArrayList<>();
            double ueWS = 0, ueTC = 0;
            for (Matiere m : ue.getMatieres()) {
                List<Grade> mg = byMatiere.getOrDefault(m.getId(), List.of());
                double fs = computeFinalScore(mg);
                mRes.add(MatiereResultResponse.builder().matiereId(m.getId()).code(m.getCode()).name(m.getName())
                        .coefficient(m.getCoefficient()).finalScore(fs).grades(mg.stream().map(mapper::toGradeResponse).toList()).build());
                ueWS += fs * m.getCoefficient(); ueTC += m.getCoefficient();
            }
            double ueAvg = ueTC > 0 ? Math.round((ueWS / ueTC) * 100.0) / 100.0 : 0.0;
            boolean validated = ueAvg >= 10.0; if (validated) totalCredits += ue.getCredits();
            genAvg += ueAvg * ue.getCoefficient(); totalW += ue.getCoefficient();

            ueResults.add(UEResultResponse.builder()
                    .teachingUnitId(ue.getId())
                    .code(ue.getCode())
                    .name(ue.getName())
                    .credits(ue.getCredits())
                    .coefficient(ue.getCoefficient())
                    .ueAverage(ueAvg)
                    .validated(validated)
                    .matieres(mRes)
                    .build()
            );
        }
        double avg = totalW > 0 ? Math.round((genAvg / totalW) * 100.0) / 100.0 : 0.0;
        Mention mention = avg >= 16 ? Mention.TRES_BIEN : avg >= 14 ? Mention.BIEN : avg >= 12 ? Mention.ASSEZ_BIEN : avg >= 10 ? Mention.PASSABLE : Mention.INSUFFISANT;

        return TranscriptResponse.builder()
                .studentId(studentId)
                .academicYear(academicYear)
                .semesterId(semesterId)
                .semesterLabel(sem != null ? sem.getLabel() : "")
                .ues(ueResults)
                .generalAverage(avg)
                .totalCredits(totalCredits)
                .mention(mention)
                .build();
    }

    private double computeFinalScore(List<Grade> grades) {
        if (grades.isEmpty()) return 0.0;
        double ws = 0, tc = 0;
        for (Grade g : grades) { double on20 = (g.getScore() / g.getEvaluation().getMaxScore()) * 20.0; ws += on20 * g.getEvaluation().getCoefficient(); tc += g.getEvaluation().getCoefficient(); }
        return tc > 0 ? Math.round((ws / tc) * 100.0) / 100.0 : 0.0;
    }
}
