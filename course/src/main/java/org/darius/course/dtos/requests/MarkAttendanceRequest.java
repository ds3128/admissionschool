package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarkAttendanceRequest {

    @NotEmpty
    private List<AttendanceEntry> entries;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AttendanceEntry {
        @NotBlank
        private String studentId;
        private boolean present;
        private String justification;
    }
}