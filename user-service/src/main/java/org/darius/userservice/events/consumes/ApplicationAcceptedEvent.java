package org.darius.userservice.events.consumes;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAcceptedEvent {

    // Identifiants
    private String applicationId;
    private String userId;
    private String studentNumber;    // généré par l'Admission Service
    private Long   filiereId;

    // Données du CandidateProfile (pour User Service)
    private String    personalEmail;
    private String    institutionalEmail;  // sera généré par Auth Service
    private String    firstName;
    private String    lastName;
    private LocalDate birthDate;
    private String    birthPlace;
    private String    nationality;
    private String    gender;
    private String    phone;
    private String    address;
    private String    photoUrl;

    // Données académiques
    private String currentInstitution;
    private String currentDiploma;
    private int    graduationYear;

    // Méta
    private boolean autoConfirmed;  // true si confirmation automatique
    private String  academicYear;
}