package org.darius.payment.events.consumed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterValidatedEvent {
    private String semesterId;
    private String academicYear;
    private String semester;
    private boolean lastSemester;
    private List<StudentResult> results;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentResult {
        private String studentId;
        private Double semesterAverage;
        private boolean admis;
    }
}