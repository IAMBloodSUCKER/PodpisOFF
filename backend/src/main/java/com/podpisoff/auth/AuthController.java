package com.podpisoff.auth;

import com.podpisoff.common.AuthFacade;
import com.podpisoff.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final AuthFacade authFacade;

    public AuthController(AuthService authService, CaptchaService captchaService, AuthFacade authFacade) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.authFacade = authFacade;
    }

    @PostMapping("/captcha")
    public CaptchaResponse captcha() {
        return captchaService.issueCaptcha();
    }

    @GetMapping("/username-check")
    public UsernameCheckResponse checkUsername(@RequestParam @NotBlank String username) {
        return authService.usernameCheck(username);
    }

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/recover-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recoverPassword(@RequestBody @Valid RecoverPasswordRequest request) {
        authService.recoverPassword(request);
    }

    @GetMapping("/me")
    public AuthResponse me() {
        User user = authFacade.getCurrentUser();
        return authService.toAuthResponse(user);
    }
}
