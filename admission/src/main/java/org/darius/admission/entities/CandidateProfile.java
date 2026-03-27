package org.darius.admission.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_profiles")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Données personnelles (depuis UserProfile)
    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    private LocalDate birthDate;

    @Column(length = 100)
    private String birthPlace;

    @Column(length = 100)
    private String nationality;

    @Column(length = 10)
    private String gender;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String photoUrl;

    @Column(length = 150)
    private String personalEmail;

    // Données académiques (saisies dans le dossier)
    @Column(length = 200)
    private String currentInstitution;

    @Column(length = 100)
    private String currentDiploma;

    @Column(length = 20)
    private String mention;

    private Integer graduationYear;

    // Doctorat
    @Column(columnDefinition = "TEXT")
    private String researchProject;

    @Column(length = 255)
    private String thesisDirectorName;

    @Column(columnDefinition = "TEXT")
    private String motivationLetter;

    @Column(nullable = false)
    @Builder.Default
    private boolean isComplete = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isFrozen = false;  // figé après soumission

    private LocalDateTime frozenAt;
}