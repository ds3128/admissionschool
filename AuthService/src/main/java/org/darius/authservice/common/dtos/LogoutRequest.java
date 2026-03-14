package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {
    @NotBlank
    private String refreshToken; // to blacklist refresh token
}
