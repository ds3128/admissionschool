package org.darius.userservice.events.consumes;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemesterValidatedEvent {
    private Long   semesterId;
    private String academicYear;
    // Liste des étudiants validés avec leurs résultats
    private java.util.List<StudentSemesterResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentSemesterResult {
        private String  studentId;
        private boolean admis;
        private double  average;
        private int     creditsObtained;
    }
}