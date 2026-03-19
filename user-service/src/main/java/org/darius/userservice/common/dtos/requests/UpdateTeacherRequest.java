package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.userservice.common.enums.AcademicGrade;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeacherRequest {

    @Size(max = 150)
    private String speciality;

    private AcademicGrade grade;

    private Long departmentId;

    @Size(max = 150)
    private String diploma;

    @Min(1) @Max(40)
    private Integer maxHoursPerWeek;
}