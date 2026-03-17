package org.darius.authservice.common.dtos;

import lombok.Builder;

import java.util.Map;
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private Long userId;
    private String email;
    private String roles;
    private Map<String, Object> claims;
}
