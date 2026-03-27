package org.darius.admission.common.dtos.responses;

import lombok.*;
import org.darius.admission.common.enums.DocumentStatus;
import org.darius.admission.common.enums.DocumentType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {
    private Long id;
    private DocumentType type;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private DocumentStatus status;
    private String rejectionReason;
    private LocalDateTime uploadedAt;
}