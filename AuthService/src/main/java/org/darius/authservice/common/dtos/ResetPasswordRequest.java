package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank @Size(min = 8)
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
