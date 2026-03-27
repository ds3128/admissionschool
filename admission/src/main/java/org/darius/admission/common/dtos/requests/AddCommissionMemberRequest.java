package org.darius.admission.common.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.darius.admission.common.enums.MemberRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddCommissionMemberRequest {

    @NotBlank(message = "L'enseignant est obligatoire")
    private String teacherId;

    @NotNull(message = "Le rôle est obligatoire")
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;
}