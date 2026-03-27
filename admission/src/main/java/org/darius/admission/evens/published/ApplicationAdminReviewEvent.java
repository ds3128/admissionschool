package org.darius.admission.evens.published;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAdminReviewEvent {
    private String applicationId;
    private String userId;
    private String personalEmail;
    // true = PENDING_COMMISSION, false = ADDITIONAL_DOCS_REQUIRED
    private boolean approved;
    private String comment;
}