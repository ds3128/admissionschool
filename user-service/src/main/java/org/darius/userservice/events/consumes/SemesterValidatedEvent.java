package org.darius.userservice.events.consumes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemesterValidatedEvent {
    private Long   semesterId;
    private String academicYear;
    private List<StudentSemesterResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StudentSemesterResult {
        private String  studentId;
        private boolean admis;
        private double  average;
        private int     creditsObtained;
    }
}