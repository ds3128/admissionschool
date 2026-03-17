package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivationAccountRequest {
    @NotBlank
    private String token;
}
