package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.Mention;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TranscriptResponse {
    private String studentId;
    private String academicYear;
    private Long semesterId;
    private String semesterLabel;
    private List<UEResultResponse> ues;
    private double generalAverage;
    private int totalCredits;
    private Mention mention;
}