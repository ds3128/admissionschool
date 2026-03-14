package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {
    @NotBlank
    private String oldPassword;
    @NotBlank @Size(min = 8)
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
