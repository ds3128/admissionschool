package org.darius.authservice.common.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
}
