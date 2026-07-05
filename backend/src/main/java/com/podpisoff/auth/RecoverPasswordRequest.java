package com.podpisoff.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecoverPasswordRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank String recoveryKey,
    @NotBlank @Size(min = 8, max = 100) String newPassword,
    @NotBlank String captchaId,
    @NotBlank String captchaAnswer
) {
}
