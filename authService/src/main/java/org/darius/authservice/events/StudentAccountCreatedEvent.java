package org.darius.authservice.events;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAccountCreatedEvent {
    private String userId;              // ID du compte auth (inchangé)
    private String institutionalEmail;  // email institutionnel
    private String tempPassword;        // mot de passe provisoire
    private String firstName;
    private String lastName;
    private String studentNumber;
}