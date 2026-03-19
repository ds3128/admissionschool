package org.darius.userservice.events.produces;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGraduatedEvent {
    private String studentId;
    private String userId;
    private Long   filiereId;
    private String filiereName;
    private String academicYear;
    // Données pour la génération du diplôme (Document Service)
    private String firstName;
    private String lastName;
}