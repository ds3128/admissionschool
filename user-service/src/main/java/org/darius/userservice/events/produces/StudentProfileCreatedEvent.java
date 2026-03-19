package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileCreatedEvent {
    private String studentId;
    private String userId;
    private Long   filiereId;
    private Long   levelId;
    private String studentNumber;
    private String academicYear;
    // Données utiles au Course Service pour créer le StudentGroup
    private String firstName;
    private String lastName;
}