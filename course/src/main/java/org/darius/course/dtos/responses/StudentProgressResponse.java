package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.Mention;
import org.darius.course.enums.ProgressStatus;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentProgressResponse {
    private Long id;
    private String studentId;
    private Long semesterId;
    private String semesterLabel;
    private double semesterAverage;
    private int creditsObtained;
    private ProgressStatus status;
    private Mention mention;
    private int rank;
    private boolean isAdmis;
    private LocalDateTime computedAt;
}
