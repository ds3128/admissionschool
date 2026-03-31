package org.darius.userservice.common.dtos.responses;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkPromoteResponse {
    private int totalRequested;
    private int promoted;
    private int skipped;           // déjà au niveau max (graduation)
    private int failed;
    private List<String> errors;
}
