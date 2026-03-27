package org.darius.course.dtos.responses;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeacherLoadResponse {
    private String teacherId;
    private Long semesterId;
    private String semesterLabel;
    private int totalHours;
    private int cmHours;
    private int tdHours;
    private int tpHours;
    private List<MatiereResponse> matieres;
}