package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ForgotPasswordRequest {
    @NotBlank @Email
    private String email;
}
