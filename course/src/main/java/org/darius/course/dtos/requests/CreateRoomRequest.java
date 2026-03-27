package org.darius.course.dtos.requests;

import jakarta.validation.constraints.*;
import lombok.*;
import org.darius.course.enums.RoomType;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateRoomRequest {

    @NotBlank
    private String name;

    private String building;

    @Min(1)
    private int capacity;

    @NotNull
    private RoomType type;

    private List<String> equipment;
}