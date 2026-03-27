package org.darius.admission.common.dtos.responses;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String birthPlace;
    private String nationality;
    private String gender;
    private String phone;
    private String address;
    private String photoUrl;
    private String personalEmail;
    private String currentInstitution;
    private String currentDiploma;
    private String mention;
    private Integer graduationYear;
    private String researchProject;
    private String thesisDirectorName;
    private boolean isComplete;
    private boolean isFrozen;
}