package org.darius.notification.events.course;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemesterValidatedEvent {
    private Long               semesterId;
    private String             semesterLabel;
    private String             academicYear;
    private boolean            isLastSemester;
    private List<StudentResult> results;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StudentResult {
        private String  studentId;
        private double  semesterAverage;
        private int     creditsObtained;
        // ADMIS, AJOURNE, EXCLUS
        private String  status;
        // TRES_BIEN, BIEN, ASSEZ_BIEN, PASSABLE, INSUFFISANT
        private String  mention;
        private boolean isAdmis;
        private int     rank;
    }
}
