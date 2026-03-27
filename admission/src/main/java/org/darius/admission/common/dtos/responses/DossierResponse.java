package org.darius.admission.common.dtos.responses;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DossierResponse {
    private Long id;
    private boolean isComplete;
    private boolean isLocked;
    private LocalDateTime lockedAt;
    private List<DocumentResponse> documents;
}