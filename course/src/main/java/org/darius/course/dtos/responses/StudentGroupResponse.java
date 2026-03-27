package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.GroupType;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentGroupResponse {
    private Long id;
    private String name;
    private GroupType type;
    private Long levelId;
    private Long filiereId;
    private Long semesterId;
    private int maxSize;
    private int currentSize;
    private List<String> studentIds;
}
