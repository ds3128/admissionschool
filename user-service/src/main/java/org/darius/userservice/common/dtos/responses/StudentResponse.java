package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.StudentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentResponse {
    private String id;
    private String profileId;
    // Profil personnel intégré
    private String firstName;
    private String lastName;
    private String avatarUrl;
    // Données académiques
    private String studentNumber;
    private int enrollmentYear;
    private Long filiereId;
    private String filiereName;
    private Long currentLevelId;
    private String currentLevelLabel;
    private StudentStatus status;
    private String admissionApplicationId;
    private List<StudentAcademicHistoryResponse> academicHistory;
    private LocalDateTime createdAt;
}