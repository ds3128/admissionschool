package org.darius.userservice.common.dtos.responses;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResultResponse {
    private String id;
    private String profileId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String identifier;   // matricule, numéro employé ou numéro personnel
    private String userType;     // STUDENT, TEACHER, STAFF
    private String contextInfo;  // filière pour étudiant, département pour enseignant
}