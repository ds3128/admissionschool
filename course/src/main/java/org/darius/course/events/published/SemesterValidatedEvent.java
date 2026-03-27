package org.darius.course.events.published;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SemesterValidatedEvent {

    private Long semesterId;
    private String semesterLabel;
    private String academicYear;

    // true → Payment Service déclenche renouvellement bourses mérite
    private boolean isLastSemester;

    private List<StudentResult> results;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StudentResult {
        private String studentId;
        private double semesterAverage;
        private int creditsObtained;
        private String status;       // ADMIS, AJOURNE, EXCLUS
        private String mention;      // TRES_BIEN, BIEN, ASSEZ_BIEN, PASSABLE, INSUFFISANT
        private boolean isAdmis;
        private int rank;
    }
}