package org.darius.userservice.common.dtos.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String headTeacherId;
    private int teacherCount;
    private int filiereCount;
    private LocalDateTime createdAt;
}