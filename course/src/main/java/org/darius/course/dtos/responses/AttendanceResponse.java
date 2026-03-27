package org.darius.course.dtos.responses;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceResponse {
    private Long id;
    private Long sessionId;
    private String studentId;
    private boolean present;
    private String justification;
    private LocalDateTime justifiedAt;
    private LocalDateTime createdAt;
}