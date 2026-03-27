package org.darius.course.dtos.responses;

import lombok.*;
import org.darius.course.enums.RoomType;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomResponse {
    private Long id;
    private String name;
    private String building;
    private int capacity;
    private RoomType type;
    private List<String> equipment;
    private boolean isAvailable;
}