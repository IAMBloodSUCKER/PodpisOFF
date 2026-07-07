package com.podpisoff.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminNotifyUserRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 1000) String body
) {
}
