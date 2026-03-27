package org.darius.admission.common.dtos.requests;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteInterviewRequest {
    private String notes;  // confidentiel
}
