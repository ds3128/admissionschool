package org.darius.admission.common.dtos.requests;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCandidateProfileRequest {

    private String currentInstitution;
    private String currentDiploma;
    private String mention;
    private Integer graduationYear;
    private String researchProject;
    private String thesisDirectorName;
    private String motivationLetter;
}