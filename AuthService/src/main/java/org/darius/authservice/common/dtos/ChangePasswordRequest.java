package org.darius.authservice.common.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank
    private String oldPassword;

    @NotBlank(message = "Password required")
    @Size(min = 8, max = 100, message = "Password must be 8 and 100 characters")
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Password must content minimum 8 characters, one uppercase, one lowercase, one number and one special character"
    )
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
