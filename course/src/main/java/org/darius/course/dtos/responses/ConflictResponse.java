package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.ConflictType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConflictResponse {
    private ConflictType type;
    private String message;
    private Long conflictingSlotId;
}
