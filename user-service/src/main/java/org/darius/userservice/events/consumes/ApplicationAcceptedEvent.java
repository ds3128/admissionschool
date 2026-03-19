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
    private String userId;                  // créé par l'Auth Service
    private String studentNumber;           // généré par l'Admission Service
    private Long   filiereId;

    // Credentials générés par l'Auth Service
    private String personalEmail;           // email personnel du candidat
    private String institutionalEmail;      // email institutionnel généré

    // CandidateProfile — données saisies durant la candidature
    private String    firstName;
    private String    lastName;
    private LocalDate birthDate;
    private String    birthPlace;
    private String    nationality;
    private String    gender;               // valeur de l'enum Gender en String
    private String    phone;
    private String    address;
    private String    photoUrl;
    private String    currentInstitution;
    private String    currentDiploma;
    private int       graduationYear;
}