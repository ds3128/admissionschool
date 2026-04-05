package org.darius.userservice.events.produces;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileCreatedEvent {
    private String studentId;
    private String userId;
    private Long   filiereId;
    private Long   levelId;
    private String studentNumber;
    private String academicYear;
    private String firstName;
    private String lastName;
}