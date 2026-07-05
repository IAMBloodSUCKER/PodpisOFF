package com.podpisoff.auth;

import com.podpisoff.user.LocaleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(min = 8, max = 100) String password,
    @Size(max = 255) String email,
    @NotBlank @Size(max = 64) String timezone,
    @NotNull LocaleCode locale,
    boolean termsAccepted,
    @NotBlank String captchaId,
    @NotBlank String captchaAnswer
) {
}
