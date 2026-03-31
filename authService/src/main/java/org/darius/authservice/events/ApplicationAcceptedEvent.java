package org.darius.authservice.events;

import lombok.*;

import java.time.LocalDate;

/**
 * Événement publié par l'Admission Service quand une candidature est acceptée.
 * L'Auth Service le consomme pour promouvoir le compte CANDIDATE → STUDENT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAcceptedEvent {

    private String applicationId;
    private String userId;            // ID du compte Auth Service (rôle CANDIDATE)
    private String studentNumber;     // Matricule généré par Admission Service
    private Long   filiereId;

    private String personalEmail;     // Email personnel du candidat
    private String institutionalEmail;// Email institutionnel (généré par Admission / Auth)

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    private String gender;
    private String phone;
    private String address;
    private String photoUrl;
    private String currentInstitution;
    private String currentDiploma;
    private int    graduationYear;
}
