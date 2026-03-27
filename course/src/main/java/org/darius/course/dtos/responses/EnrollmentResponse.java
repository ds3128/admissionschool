package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.EnrollStatus;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EnrollmentResponse {
    private Long id;
    private String studentId;
    private Long matiereId;
    private String matiereName;
    private String teachingUnitName;
    private Long groupId;
    private String groupName;
    private Long semesterId;
    private LocalDate enrolledAt;
    private EnrollStatus status;
}