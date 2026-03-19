package org.darius.userservice.common.dtos.responses;

import lombok.*;
import org.darius.userservice.common.enums.AcademicGrade;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeacherSummaryResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String employeeNumber;
    private String speciality;
    private AcademicGrade grade;
    private String departmentName;
    private boolean isActive;
}