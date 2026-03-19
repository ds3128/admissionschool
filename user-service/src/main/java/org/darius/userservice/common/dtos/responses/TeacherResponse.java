package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.AcademicGrade;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeacherResponse {
    private String id;
    private String profileId;
    private String userId;
    // Profil personnel intégré
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String personalEmail;
    // Données professionnelles
    private String employeeNumber;
    private String speciality;
    private AcademicGrade grade;
    private Long departmentId;
    private String departmentName;
    private String diploma;
    private int maxHoursPerWeek;
    private boolean isActive;
    private LocalDateTime createdAt;
}