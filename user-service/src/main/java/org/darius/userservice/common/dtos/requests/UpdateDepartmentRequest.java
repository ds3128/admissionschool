package org.darius.userservice.common.dtos.requests;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDepartmentRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;
}